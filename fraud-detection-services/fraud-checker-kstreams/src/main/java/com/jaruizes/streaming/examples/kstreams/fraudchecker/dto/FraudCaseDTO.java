package com.jaruizes.streaming.examples.kstreams.fraudchecker.dto;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class FraudCaseDTO {

    public final static int MULTIPLE_ATM_MOVEMENTS_IN_SHORT_PERIOD = 1;
    public final static int ATM_AND_MERCHANT_IN_SHORT_PERIOD = 2;
    public final static int MULTIPLE_MERCHANT_MOVEMENTS_IN_SHORT_PERIOD = 3;
    public final static int MULTIPLE_ONLINE_MOVEMENTS_IN_SHORT_PERIOD = 4;

    private int type;
    private boolean fraudDetected;
    private Set<CardMovementDTO> cardMovements;

    public FraudCaseDTO() {
        this.cardMovements = new HashSet<>();
        this.fraudDetected = false;
    }

    public FraudCaseDTO(int type) {
        this();
        this.type = type;
    }

    public void addATMMovement(ATMMovementDTO atmMovementDTO) {
        cardMovements.add(atmMovementDTO);
    }
}
