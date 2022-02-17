package com.paradigma.rt.streaming.cardmovements.loader.api.rest.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnlineMovementDTO extends CardMovementDTO {

    private String site;
}
