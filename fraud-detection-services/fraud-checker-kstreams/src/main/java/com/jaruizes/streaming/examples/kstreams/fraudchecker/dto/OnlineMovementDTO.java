package com.jaruizes.streaming.examples.kstreams.fraudchecker.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class OnlineMovementDTO extends CardMovementDTO {

    private String site;
}
