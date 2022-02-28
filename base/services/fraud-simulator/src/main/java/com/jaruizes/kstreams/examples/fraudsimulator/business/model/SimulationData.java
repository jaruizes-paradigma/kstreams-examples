package com.jaruizes.kstreams.examples.fraudsimulator.business.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SimulationData {

    private int iterations;
    private long msBetweenIterations;
    private long id;
    private List<CardFraudConfig> cardsFraudConfig;
}
