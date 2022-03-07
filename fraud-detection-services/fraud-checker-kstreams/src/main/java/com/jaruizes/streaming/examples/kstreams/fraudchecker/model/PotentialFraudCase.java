package com.jaruizes.streaming.examples.kstreams.fraudchecker.model;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({@JsonSubTypes.Type(value = PotentialATMFraudCase.class)})
@Data
public class PotentialFraudCase {

    private boolean isFraud;
    private String card;
    private int type;
    private long id;
    private List<CardMovement> movements;

    public PotentialFraudCase() {
        this.id = new Date().getTime();

//        System.out.println("---> Created new Fraud Case: " + getId());
        this.isFraud = false;
        this.movements = new ArrayList<>();
    }

    public void addMovement(CardMovement currentMovement) {
        this.movements.add(currentMovement);
    }

    @Override
    public String toString() {
        String movsToString = "\n";
        movements.forEach((m) -> movsToString.concat("\t -> Id: " + m.getId() + " / Origin: " + m.getOrigin() + "\n"));

        return "------> PotentialFraudCase{" + id + "): " +
                "isFraud=" + isFraud +
                ", card='" + card + '\'' +
                ", movements= " + movsToString;
    }
}
