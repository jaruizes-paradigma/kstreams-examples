package com.paradigma.rt.streaming.cardmovements.loader.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
public class CardMovement {

    private String id;
    private String card;
    private Date createdAt;
    private float amount;


}
