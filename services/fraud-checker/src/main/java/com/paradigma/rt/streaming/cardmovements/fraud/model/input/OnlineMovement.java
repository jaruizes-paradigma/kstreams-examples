package com.paradigma.rt.streaming.cardmovements.fraud.model.input;

import com.paradigma.rt.streaming.cardmovements.fraud.model.base.CardMovement;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@ToString
public class OnlineMovement implements CardMovement {

    @EqualsAndHashCode.Include private String id;
    private String card;
    private Date createdAt;
    private float amount;
    private String site;

    @Builder
    public OnlineMovement(String card, float amount, String site) {
        this.card = card;
        this.amount = amount;
        this.site = site;
        this.createdAt = new Date();
        this.id = "atm-" + this.createdAt.getTime();
    }
}
