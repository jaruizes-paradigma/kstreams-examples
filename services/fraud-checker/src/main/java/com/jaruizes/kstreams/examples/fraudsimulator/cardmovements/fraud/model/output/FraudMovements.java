package com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.model.output;

import com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.model.base.CardMovement;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class FraudMovements {

    private String card;
    private int type;
    private Set<CardMovement> cardMovements;

    public FraudMovements() {
        this.cardMovements = new HashSet<>();
    }

    public FraudMovements(int type) {
        this.type = type;
        this.cardMovements = new HashSet<>();
    }

    public void addMovement(CardMovement cardMovement) {
        this.cardMovements.add(cardMovement);
    }
}
