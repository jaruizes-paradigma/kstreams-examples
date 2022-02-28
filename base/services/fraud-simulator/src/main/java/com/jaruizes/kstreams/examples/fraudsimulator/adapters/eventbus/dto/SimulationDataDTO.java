package com.jaruizes.kstreams.examples.fraudsimulator.adapters.eventbus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SimulationDataDTO {
    private List<CardFraudConfigDTO> cardFraudConfig;
    private int iterations;
    private long msBetweenIterations;
    private long id;
}
