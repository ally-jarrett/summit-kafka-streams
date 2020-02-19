package com.redhat.summit.test.camel;

import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.model.TransactionEventConfiguration;
import com.redhat.summit.processor.TransactionEventProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TransactionEventProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:out")
    protected MockEndpoint out;

    @Produce(uri = "direct:start")
    protected ProducerTemplate testProducer;

    /**
     * Test in CamelContext rather than invoking bean directly
     * @return
     * @throws Exception
     */
    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").process(new TransactionEventProcessor()).to(out);
            }
        };
    }

    @Test
    public void testEventConfigurationBasic() throws Exception {
        TransactionEventConfiguration eventConfiguration = this.getEventConfiguration();
        testProducer.sendBody(eventConfiguration);

        out.setExpectedMessageCount(100);
        Long delay = (Long) out.getExchanges().get(0).getIn().getHeader("delayTimer");
        assertNotNull(delay);
        assertEquals(0L, delay.longValue());
    }

    @Test
    public void testEventConfigurationTimeFrameSeconds() throws Exception {
        int timeframe = 100;
        TransactionEventConfiguration eventConfiguration = this.getEventConfiguration();
        eventConfiguration.setTimeframe(timeframe + "s");
        testProducer.sendBody(eventConfiguration);

        out.setExpectedMessageCount(100);
        Long delay = (Long) out.getExchanges().get(0).getIn().getHeader("delayTimer");
        long timeInMillis = TimeUnit.SECONDS.toMillis(timeframe);
        long delta = timeInMillis / eventConfiguration.getTotalTransactionCount();
        assertNotNull(delay);
        assertEquals(delta, delay.longValue());
    }

    @Test
    public void testEventConfigurationTimeFrameMinutes() throws Exception {
        int timeframe = 1;
        TransactionEventConfiguration eventConfiguration = this.getEventConfiguration();
        eventConfiguration.setTimeframe(timeframe + "m");
        testProducer.sendBody(eventConfiguration);

        out.setExpectedMessageCount(eventConfiguration.getTotalTransactionCount());
        Long delay = (Long) out.getExchanges().get(0).getIn().getHeader("delayTimer");
        long timeInMillis = TimeUnit.MINUTES.toMillis(timeframe);
        long delta = timeInMillis / eventConfiguration.getTotalTransactionCount();
        assertNotNull(delay);
        assertEquals(delta, delay.longValue());
    }

    @Test
    public void testEventConfigurationMaxTimeFrame10Minutes() throws Exception {
        int timeframe = 1;
        TransactionEventConfiguration eventConfiguration = this.getEventConfiguration();
        eventConfiguration.setTimeframe(timeframe + "h");
        testProducer.sendBody(eventConfiguration);

        out.setExpectedMessageCount(eventConfiguration.getTotalTransactionCount());
        Long delay = (Long) out.getExchanges().get(0).getIn().getHeader("delayTimer");
        long timeInMillis = TimeUnit.MINUTES.toMillis(10);
        long delta = timeInMillis / eventConfiguration.getTotalTransactionCount();
        assertNotNull(delay);
        assertEquals(delta, delay.longValue());
    }


    @Test
    public void testEventConfigurationCustomTx() throws Exception {
        int timeframe = 1;
        TransactionEventConfiguration eventConfiguration = this.getEventConfiguration();
        eventConfiguration.setTimeframe(timeframe + "m");

        // Add 2 Custom Specified TX
        CreditCardTransaction ccTx = new CreditCardTransaction();
        ccTx.setTransactionReference(UUID.randomUUID().toString());
        ccTx.setCardHolderName(eventConfiguration.getCardHolderName());
        ccTx.setCardNumber(eventConfiguration.getCardNumber());
        ccTx.setAmount(1000l);

        CreditCardTransaction ccTx1 = new CreditCardTransaction();
        ccTx1.setTransactionReference(UUID.randomUUID().toString());
        ccTx1.setCardHolderName(eventConfiguration.getCardHolderName());
        ccTx1.setCardNumber(eventConfiguration.getCardNumber());
        ccTx1.setAmount(10000l);

        List<CreditCardTransaction> txs = new ArrayList<>();
        txs.add(ccTx);
        txs.add(ccTx1);
        eventConfiguration.setCustomTransactions(txs);
        testProducer.sendBody(eventConfiguration);

        out.setExpectedMessageCount(eventConfiguration.getTotalTransactionCount() + txs.size());
        Long delay = (Long) out.getExchanges().get(0).getIn().getHeader("delayTimer");
        long timeInMillis = TimeUnit.MINUTES.toMillis(1);
        long totalTxs = eventConfiguration.getTotalTransactionCount() + txs.size();
        long delta = timeInMillis / totalTxs;
        assertNotNull(delay);
        assertEquals(delta, delay.longValue());
    }

    private TransactionEventConfiguration getEventConfiguration() {
        TransactionEventConfiguration eventConfiguration = new TransactionEventConfiguration();
        eventConfiguration.setCardHolderName("Ally");
        eventConfiguration.setCardNumber("1234-5678-9101");
        eventConfiguration.setTotalAmount(1000l);
        eventConfiguration.setTotalTransactionCount(100);
        return eventConfiguration;
    }
}