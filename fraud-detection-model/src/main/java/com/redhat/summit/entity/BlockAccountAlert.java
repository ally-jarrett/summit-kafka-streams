package com.redhat.summit.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class BlockAccountAlert {

    private Long cardId;

    public BlockAccountAlert(Long cardId) {
        this.cardId = cardId;
    }

    public Long getCardId() {
        return cardId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("cardId", cardId)
                .toString();
    }
}
