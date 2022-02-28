package com.jaruizes.kstreams.examples.fraudsimulator.api.async;

import com.jaruizes.kstreams.examples.fraudsimulator.adapters.eventbus.dto.SimulationDataDTO;
import com.jaruizes.kstreams.examples.fraudsimulator.business.FraudSimulator;
import com.jaruizes.kstreams.examples.fraudsimulator.business.model.CardFraudConfig;
import com.jaruizes.kstreams.examples.fraudsimulator.business.model.SimulationData;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.stream.Collectors;

@ApplicationScoped
public class FraudSimulatorAsyncAPI {

    private final Logger logger = Logger.getLogger(FraudSimulatorAsyncAPI.class);

    @Inject
    FraudSimulator fraudSimulator;

    @Incoming("datagen-fraudsimulator-in")
    @Blocking
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)
    public Uni<Void> startFraudSimulation(Message<SimulationDataDTO> record) {
        this.logger.info("[ASYNC] Creating fraud movements....");
        SimulationDataDTO simulationDataDTO = record.getPayload();
        this.fraudSimulator.executeFraudSimulationProcess(
                SimulationData.builder()
                        .iterations(simulationDataDTO.getIterations())
                        .msBetweenIterations( simulationDataDTO.getMsBetweenIterations())
                        .id(simulationDataDTO.getId())
                        .cardsFraudConfig(simulationDataDTO.getCardFraudConfig()
                                .stream()
                                .map(cardFraudConfigDTO -> CardFraudConfig
                                        .builder()
                                        .card(cardFraudConfigDTO.getCard())
                                        .fraudTypes(cardFraudConfigDTO.getFraudTypes())
                                        .build())
                                .collect(Collectors.toList()))
                        .build());

        return Uni.createFrom().voidItem();
    }
}
