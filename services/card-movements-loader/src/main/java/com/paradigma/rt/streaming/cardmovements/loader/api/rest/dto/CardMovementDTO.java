package com.paradigma.rt.streaming.cardmovements.loader.api.rest.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardMovementDTO {

    private float amount;
    private String card;
}
