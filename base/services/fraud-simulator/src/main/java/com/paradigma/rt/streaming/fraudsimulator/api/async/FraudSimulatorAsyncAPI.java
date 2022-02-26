package com.paradigma.rt.streaming.fraudsimulator.api.async;

import com.paradigma.rt.streaming.fraudsimulator.api.rest.dto.StartFraudSimulationDTO;
import com.paradigma.rt.streaming.fraudsimulator.business.FraudSimulator;
import com.paradigma.rt.streaming.fraudsimulator.model.SimulationData;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class FraudSimulatorAsyncAPI {

    private final Logger logger = Logger.getLogger(FraudSimulatorAsyncAPI.class);

    @Inject
    FraudSimulator fraudSimulator;

    @Incoming("datagen-fraudsimulator-in")
    @Blocking
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)
    public Uni<Void> startFraudSimulation(Message<StartFraudSimulationDTO> record) {
        this.logger.info("[ASYNC] Creating fraud movements....");
        StartFraudSimulationDTO startFraudSimulationDTO = record.getPayload();
        this.fraudSimulator.executeFraudSimulationProcess(
                SimulationData.builder()
                        .cards(startFraudSimulationDTO.getCards())
                        .iterations(startFraudSimulationDTO.getIterations())
                        .msBetweenIterations( startFraudSimulationDTO.getMsBetweenIterations())
                        .id(startFraudSimulationDTO.getId())
                        .build());

        return Uni.createFrom().voidItem();
    }
}
