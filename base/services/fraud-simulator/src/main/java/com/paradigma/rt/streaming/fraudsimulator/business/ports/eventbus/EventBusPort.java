package com.paradigma.rt.streaming.fraudsimulator.business.ports.eventbus;

import com.paradigma.rt.streaming.fraudsimulator.business.model.SimulationData;
import com.paradigma.rt.streaming.fraudsimulator.business.model.SimulationDataResults;

public interface EventBusPort {

    void publishSimulationData(SimulationData simulationData);
    void publishSimulationDataResults(SimulationDataResults simulationDataResults);
    void publishATMMovement(String card, String atm, float amount, long processId, int iteration);
    void publishOnlineMovement(String card, String site, float amount, long processId, int iteration);
    void publishMerchantMovement(String card, String merchant, float amount, long processId, int iteration);
}
