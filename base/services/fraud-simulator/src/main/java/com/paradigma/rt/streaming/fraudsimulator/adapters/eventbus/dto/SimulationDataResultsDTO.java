package com.paradigma.rt.streaming.fraudsimulator.adapters.eventbus.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimulationDataResultsDTO {
    private long id;
    private int cards;
    private int iterations;
    private long msBetweenIterations;
    private int movementsGenerated;
    private int potentialFraudMovements;
}
