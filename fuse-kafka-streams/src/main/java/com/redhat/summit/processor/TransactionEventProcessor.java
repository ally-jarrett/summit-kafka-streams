package com.redhat.summit.processor;

import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.model.TransactionEventConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Component
@Slf4j
public class TransactionEventProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        TransactionEventConfiguration eventConfig = (TransactionEventConfiguration) exchange.getIn().getBody();
        log.info("Event Configuration : {}", eventConfig);

        if (eventConfig != null) {
            int txCount = eventConfig.getTotalTransactionCount() <= 0 ? 1 : eventConfig.getTotalTransactionCount();
            long totalTxAmount = eventConfig.getTotalAmount() <= 0 ? 10000 : eventConfig.getTotalAmount();

            log.info("Generating {} Transactions with a Total TX Amount of : {} (Not including specified Custom TXs)", txCount, totalTxAmount);
            List<CreditCardTransaction> transactions = new ArrayList<>();

            // Add Custom Transactions
            if (!CollectionUtils.isEmpty(eventConfig.getCustomTransactions())) {
                log.info("Adding {} Custom Transactions ", eventConfig.getCustomTransactions().size());
                eventConfig.getCustomTransactions().stream().forEach(t -> {
                    transactions.add(this.generateCCTXTemplate(
                            eventConfig.getCardHolderName(),
                            eventConfig.getCardNumber(),
                            t.getAmount()));
                });
            }

            // Generate Transactions
            Long totalTxAmountIncCustom = totalTxAmount + transactions.size();
            Long txAmount = totalTxAmount / txCount;
            AtomicInteger txAmountApplied = new AtomicInteger(0);
            log.info("Setting each TX with a avg txAmount : {}", txAmount);

            IntStream.range(0, txCount).forEach(idx -> {
                if (idx == txCount - 1) {
                    Long amount = totalTxAmount - txAmountApplied.get();

                    log.info("Add txAmount: {} delta to final TX", amount);
                    transactions.add(this.generateCCTXTemplate(
                            eventConfig.getCardHolderName(),
                            eventConfig.getCardNumber(),
                            amount));

                    log.info("Total txAmount: {} added", txAmountApplied.get() + amount);
                } else {
                    txAmountApplied.getAndAdd(txAmount.intValue());
                    transactions.add(this.generateCCTXTemplate(
                            eventConfig.getCardHolderName(),
                            eventConfig.getCardNumber(),
                            txAmount));
                }
            });

            // Expects String :: Xs or Xm i.e. 10s, 10m, consider ISO 8601
            exchange.getIn().getHeaders().put("delayTimer", 0l);

            if (StringUtils.isNotBlank(eventConfig.getTimeframe())) {

                String var = eventConfig.getTimeframe().trim().toLowerCase();
                log.info("Processing Timeframe value of : {}", var);
                long interval = Long.valueOf(var.substring(0, var.length() - 1)).longValue();
                long timeframe = 0l;
                char c = var.charAt(var.length() - 1);

                log.info("Interval : {} :: Timeframe Type : {}", interval, c);
                // Parse timeframe
                switch (c) {
                    case 'h':
                        log.debug("Setting timeframe type to HOURS");
                        timeframe = TimeUnit.HOURS.toMillis(interval);
                        break;
                    case 'm':
                        log.debug("Setting timeframe type to MINUTES");
                        timeframe = TimeUnit.MINUTES.toMillis(interval);
                        break;
                    case 's':
                        log.debug("Setting timeframe type to SECONDS");
                        timeframe = TimeUnit.SECONDS.toMillis(interval);
                        break;
                    default:
                        log.info("Timeframe unparsable, defaulting to 0");
                }

                // Limit maximum producer time to 10 mins
                if (timeframe > TimeUnit.MINUTES.toMillis(10)) {
                    timeframe = TimeUnit.MINUTES.toMillis(10);
                }

                if (timeframe > 0) {
                    log.info("Timeframe of {} millis calculated, dividing by total transaction count: {} ", timeframe, transactions.size());
                    exchange.getIn().getHeaders().put("delayTimer", timeframe / transactions.size());
                }

                log.info("Each Transaction will have an interval of {} millis", exchange.getIn().getHeader("delayTimer"));
            }

            log.info("Total of {} Transactions Generated for Kafka Events", transactions.size());
            transactions.stream().forEach(t -> log.info("TX: {}", t));
            exchange.getIn().setBody(transactions);
        } else {
            log.error("Config is null, unable to generate TX's");

            // TODO Add proper error handling
            throw new Exception("No config..");
        }
    }

    private CreditCardTransaction generateCCTXTemplate(String name, String id, long amount) {
        CreditCardTransaction tx = new CreditCardTransaction();
        tx.setCardHolderName(name);
        tx.setAmount(amount);
        tx.setCardNumber(id);
        tx.setTransactionReference(UUID.randomUUID().toString());
        return tx;
    }

}