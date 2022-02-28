package com.jaruizes.kstreams.examples.fraudsimulator.api.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StartFraudSimulationDTO {

    private long id;
    private int iterations;
    private long msBetweenIterations;
    private List<CardFraudConfigDTO> cardsFraudConfig;
}
