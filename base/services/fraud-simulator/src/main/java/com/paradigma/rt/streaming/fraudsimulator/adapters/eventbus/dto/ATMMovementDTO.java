package com.paradigma.rt.streaming.fraudsimulator.adapters.eventbus.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class ATMMovementDTO extends CardMovementDTO {

    private String atm;
}
