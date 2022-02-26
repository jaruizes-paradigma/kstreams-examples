package com.paradigma.rt.streaming.fraudsimulator.adapters.eventbus.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimulationDataDTO {
    private int cards;
    private int iterations;
    private long msBetweenIterations;
    private long id;
}
