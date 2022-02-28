package com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.model.input;

import com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.model.base.CardMovement;
import lombok.*;

import java.util.Date;

@Data
@ToString
public class MerchantMovement implements CardMovement {

    @EqualsAndHashCode.Include private String id;
    private String card;
    private Date createdAt;
    private float amount;
    private String merchant;

    @Builder
    public MerchantMovement(String card, float amount, String merchant) {
        this.card = card;
        this.amount = amount;
        this.merchant = merchant;
        this.createdAt = new Date();
        this.id = "atm-" + this.createdAt.getTime();
    }
}
