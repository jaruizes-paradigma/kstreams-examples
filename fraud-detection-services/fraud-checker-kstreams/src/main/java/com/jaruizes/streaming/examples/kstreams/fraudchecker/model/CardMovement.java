package com.jaruizes.streaming.examples.kstreams.fraudchecker.model;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class CardMovement {
    private String id;
    private String card;
    private Date createdAt;
    private float amount;
    private int type;
    private String origin;
    private boolean isFraud;
}
