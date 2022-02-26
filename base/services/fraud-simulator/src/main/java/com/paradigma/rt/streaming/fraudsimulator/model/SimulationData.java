package com.paradigma.rt.streaming.fraudsimulator.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimulationData {

    private int cards;
    private int iterations;
    private long msBetweenIterations;
    private long id;
}
