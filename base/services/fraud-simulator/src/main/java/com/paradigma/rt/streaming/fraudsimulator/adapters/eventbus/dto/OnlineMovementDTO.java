package com.paradigma.rt.streaming.fraudsimulator.adapters.eventbus.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class OnlineMovementDTO extends CardMovementDTO {

    private String site;
}
