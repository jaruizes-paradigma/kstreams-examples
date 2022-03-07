package com.jaruizes.streaming.examples.kstreams.fraudchecker.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
public class CardMovementDTO {

    private String id;
    private String card;
    private Date createdAt;
    private float amount;
}
