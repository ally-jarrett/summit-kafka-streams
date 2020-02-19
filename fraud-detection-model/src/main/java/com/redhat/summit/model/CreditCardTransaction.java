package com.redhat.summit.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class CreditCardTransaction {

    private String transactionReference;
    private String cardHolderName;
    private String cardNumber;
    private long amount;
    private LocalDateTime date;

}
