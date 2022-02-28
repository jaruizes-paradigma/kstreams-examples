package com.jaruizes.kstreams.examples.fraudsimulator.adapters.eventbus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SimulationDataResultsDTO {
    private long id;
    private int iterations;
    private long msBetweenIterations;
    private List<CardFraudConfigDTO> cardFraudConfig;
    private int movementsGenerated;
    private int potentialFraudMovements;
}
