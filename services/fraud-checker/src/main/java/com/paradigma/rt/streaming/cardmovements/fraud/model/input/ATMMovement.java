package com.paradigma.rt.streaming.cardmovements.fraud.model.input;

import com.paradigma.rt.streaming.cardmovements.fraud.model.base.CardMovement;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@EqualsAndHashCode()
@Data
@ToString
public class ATMMovement implements CardMovement {

    @EqualsAndHashCode.Include private String id;
    private String card;
    private Date createdAt;
    private float amount;
    private String atm;

    @Builder
    public ATMMovement(String card, float amount, String atm) {
        this.card = card;
        this.amount = amount;
        this.atm = atm;
        this.createdAt = new Date();
        this.id = "atm-" + this.createdAt.getTime();
    }
}
