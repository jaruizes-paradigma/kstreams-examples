package com.jaruizes.kstreams.examples.fraudsimulator.api.rest;

import com.jaruizes.kstreams.examples.fraudsimulator.api.rest.dto.StartFraudSimulationDTO;
import com.jaruizes.kstreams.examples.fraudsimulator.api.rest.dto.StartFraudSimulationResponseDTO;
import com.jaruizes.kstreams.examples.fraudsimulator.business.FraudSimulator;
import com.jaruizes.kstreams.examples.fraudsimulator.business.model.CardFraudConfig;
import com.jaruizes.kstreams.examples.fraudsimulator.business.model.SimulationData;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.stream.Collectors;


@Path("/fraud-simulation")
public class FraudSimulatorRestAPI {

    private final Logger logger = Logger.getLogger(FraudSimulatorRestAPI.class);

    @Inject
    FraudSimulator fraudSimulator;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSimulateFraudProcess(StartFraudSimulationDTO startFraudSimulationDTO) {
        this.logger.info("[REST] Received request to start a new fraud simulation");
        long processId = this.fraudSimulator.createFraudSimulationProcess(SimulationData.builder()
                .iterations(startFraudSimulationDTO.getIterations())
                .msBetweenIterations(startFraudSimulationDTO.getMsBetweenIterations())
                .cardsFraudConfig(startFraudSimulationDTO.getCardsFraudConfig()
                        .stream()
                        .map(cardFraudConfigDTO -> CardFraudConfig
                                .builder()
                                .card(cardFraudConfigDTO.getCard())
                                .fraudTypes(cardFraudConfigDTO.getFraudTypes())
                                .build())
                        .collect(Collectors.toList()))
                .build());

        this.logger.info("[REST] Request to start a new fraud simulation initiated. ID: " + processId);
        return Response.ok(new StartFraudSimulationResponseDTO(processId))
                .status(201)
                .build();
    }

//    @POST
//    @Path("/online")
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response generate(OnlineMovementDTO onlineMovementDTO) {
//        Date now = new Date();
//        OnlineMovement onlineMovement = OnlineMovement.builder()
//                .site(onlineMovementDTO.getSite())
//                .card(onlineMovementDTO.getCard())
//                .amount(onlineMovementDTO.getAmount())
//                .createdAt(now)
//                .id("online-" + now.getTime())
//                .build();
//
//
//        this.onlineMovementEmitter.send(Message.of(onlineMovement));
//
//        return Response.ok(new CreateMovementResponseDTO(onlineMovement.getId())).status(201).build();
//    }
//
//    @POST
//    @Path("/merchant")
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response generate(MerchantMovementDTO merchantMovementDTO) {
//        Date now = new Date();
//        MerchantMovement onlineMovement = MerchantMovement.builder()
//                .merchant(merchantMovementDTO.getMerchant())
//                .card(merchantMovementDTO.getCard())
//                .amount(merchantMovementDTO.getAmount())
//                .createdAt(now)
//                .id("merchant-" + now.getTime())
//                .build();
//
//
//        this.merchantMovementEmitter.send(Message.of(onlineMovement));
//
//        return Response.ok(new CreateMovementResponseDTO(onlineMovement.getId())).status(201).build();
//    }
}