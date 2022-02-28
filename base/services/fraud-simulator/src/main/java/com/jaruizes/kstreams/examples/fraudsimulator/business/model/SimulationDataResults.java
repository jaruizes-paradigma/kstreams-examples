package com.jaruizes.kstreams.examples.fraudsimulator.business.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SimulationDataResults {

    private int iterations;
    private long msBetweenIterations;
    private long id;
    private int movementsGenerated;
    private int potentialFraudMovements;
    private List<CardFraudConfig> cardsFraudConfig;
}
