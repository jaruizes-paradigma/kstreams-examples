package com.paradigma.rt.streaming.fraudsimulator.adapters.eventbus;

import com.paradigma.rt.streaming.fraudsimulator.adapters.eventbus.dto.*;
import com.paradigma.rt.streaming.fraudsimulator.business.FraudSimulator;
import com.paradigma.rt.streaming.fraudsimulator.business.ports.eventbus.EventBusPort;
import com.paradigma.rt.streaming.fraudsimulator.business.model.SimulationData;
import com.paradigma.rt.streaming.fraudsimulator.business.model.SimulationDataResults;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Date;

@ApplicationScoped
public class EventBusPortKafka implements EventBusPort {
    private final Logger logger = Logger.getLogger(EventBusPortKafka.class);

    @Inject
    @Channel("datagen-fraudsimulator-out")
    Emitter<SimulationDataDTO> simulationDataDTOEmitter;

    @Inject
    @Channel("datagen-fraudsimulator-results")
    Emitter<SimulationDataResultsDTO> simulationDataResultsDTOEmitter;

    @Inject
    @Channel("atm-movements-out")
    Emitter<ATMMovementDTO> atmMovementEmitter;

    @Inject @Channel("online-movements-out")
    Emitter<OnlineMovementDTO> onlineMovementEmitter;

    @Inject @Channel("merchant-movements-out")
    Emitter<MerchantMovementDTO> merchantMovementEmitter;

    @Override
    public void publishSimulationData(SimulationData simulationData) {
        this.simulationDataDTOEmitter.send(Message.of(
                SimulationDataDTO.builder()
                .cards(simulationData.getCards())
                .iterations(simulationData.getIterations())
                .msBetweenIterations(simulationData.getMsBetweenIterations())
                .id(simulationData.getId())
                .build()
        ));
    }

    @Override
    public void publishSimulationDataResults(SimulationDataResults simulationDataResults) {
        this.simulationDataResultsDTOEmitter.send(Message.of(
                SimulationDataResultsDTO.builder()
                        .cards(simulationDataResults.getCards())
                        .iterations(simulationDataResults.getIterations())
                        .msBetweenIterations(simulationDataResults.getMsBetweenIterations())
                        .id(simulationDataResults.getId())
                        .movementsGenerated(simulationDataResults.getMovementsGenerated())
                        .potentialFraudMovements(simulationDataResults.getPotentialFraudMovements())
                        .build()
        ));
    }

    /**
     * Publish an ATM movement
     * @param card
     * @param atm
     * @param amount
     */
    @Override
    public void publishATMMovement(String card, String atm, float amount, long processId, int iteration) {
        String id = generateIdMovement(FraudSimulator.ATM_PREFIX, card, processId, iteration);
        this.atmMovementEmitter.send(Message.of(ATMMovementDTO.builder()
                .atm(atm)
                .card(card)
                .amount(amount)
                .createdAt(new Date())
                .id(id)
                .build()));

        this.logger.debug("ATM Movement published: " + id);
    }

    /**
     * Publish a merchant movement
     * @param card
     * @param merchant
     * @param amount
     */
    @Override
    public void publishMerchantMovement(String card, String merchant, float amount, long processId, int iteration) {
        String id = generateIdMovement(FraudSimulator.MERCHANT_PREFIX, card, processId, iteration);
        this.merchantMovementEmitter.send(Message.of(MerchantMovementDTO.builder()
                .merchant(merchant)
                .card(card)
                .amount(amount)
                .createdAt(new Date())
                .id(id)
                .build()));

        this.logger.debug("Merchant Movement published: " + id);
    }

    /**
     * Publish an online movement
     * @param card
     * @param site
     * @param amount
     */
    @Override
    public void publishOnlineMovement(String card, String site, float amount, long processId, int iteration) {
        String id = generateIdMovement(FraudSimulator.ONLINE_PREFIX, card, processId, iteration);
        this.onlineMovementEmitter.send(Message.of(OnlineMovementDTO.builder()
                .site(site)
                .card(card)
                .amount(amount)
                .createdAt(new Date())
                .id(id)
                .build()));
        this.logger.debug("Online Movement published: " + id);
    }

    private String generateIdMovement(String type, String card, long processId, int iteration) {
        return type + processId + iteration + "-" + card;
    }
}
