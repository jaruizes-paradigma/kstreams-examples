package com.paradigma.rt.streaming.fraudsimulator.api.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartFraudSimulationDTO {

    private long id;
    private int cards;
    private int iterations;
    private long msBetweenIterations;
}
