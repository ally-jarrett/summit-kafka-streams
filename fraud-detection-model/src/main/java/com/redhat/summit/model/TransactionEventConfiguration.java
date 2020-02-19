package com.redhat.summit.model;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TransactionEventConfiguration {

    private String cardHolderName;
    private String cardNumber;
    private long totalAmount;
    private int totalTransactionCount;

    // Expects String :: Xs or Xm i.e. 10s, 10m, consider ISO 8601
    // Everything else will be ignored
    private String timeframe;
    private List<CreditCardTransaction> customTransactions;

}
