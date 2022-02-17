package com.paradigma.rt.streaming.cardmovements.loader.api.rest;

import com.paradigma.rt.streaming.cardmovements.loader.api.rest.dto.ATMMovementDTO;
import com.paradigma.rt.streaming.cardmovements.loader.api.rest.dto.CreateMovementResponseDTO;
import com.paradigma.rt.streaming.cardmovements.loader.api.rest.dto.MerchantMovementDTO;
import com.paradigma.rt.streaming.cardmovements.loader.api.rest.dto.OnlineMovementDTO;
import com.paradigma.rt.streaming.cardmovements.loader.model.ATMMovement;
import com.paradigma.rt.streaming.cardmovements.loader.model.MerchantMovement;
import com.paradigma.rt.streaming.cardmovements.loader.model.OnlineMovement;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;


@Path("/cards-movements")
public class CardMovementsLoaderRest {

    private final Logger logger = Logger.getLogger(CardMovementsLoaderRest.class);

    @Inject @Channel("atm-movements-out")
    Emitter<ATMMovement> atmMovementEmitter;

    @Inject @Channel("online-movements-out")
    Emitter<OnlineMovement> onlineMovementEmitter;

    @Inject @Channel("merchant-movements-out")
    Emitter<MerchantMovement> merchantMovementEmitter;

    @POST
    @Path("/atm")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generate(ATMMovementDTO atmMovementDTO) {
        Date now = new Date();
        ATMMovement atmMovement = ATMMovement.builder()
                .atm(atmMovementDTO.getAtm())
                .card(atmMovementDTO.getCard())
                .amount(atmMovementDTO.getAmount())
                .createdAt(now)
                .id("atm-" + now.getTime())
                .build();


        this.atmMovementEmitter.send(Message.of(atmMovement));

        return Response.ok(new CreateMovementResponseDTO(atmMovement.getId()))
                .status(201).build();
    }

    @POST
    @Path("/online")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generate(OnlineMovementDTO onlineMovementDTO) {
        Date now = new Date();
        OnlineMovement onlineMovement = OnlineMovement.builder()
                .site(onlineMovementDTO.getSite())
                .card(onlineMovementDTO.getCard())
                .amount(onlineMovementDTO.getAmount())
                .createdAt(now)
                .id("online-" + now.getTime())
                .build();


        this.onlineMovementEmitter.send(Message.of(onlineMovement));

        return Response.ok(new CreateMovementResponseDTO(onlineMovement.getId())).status(201).build();
    }

    @POST
    @Path("/merchant")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generate(MerchantMovementDTO merchantMovementDTO) {
        Date now = new Date();
        MerchantMovement onlineMovement = MerchantMovement.builder()
                .merchant(merchantMovementDTO.getMerchant())
                .card(merchantMovementDTO.getCard())
                .amount(merchantMovementDTO.getAmount())
                .createdAt(now)
                .id("merchant-" + now.getTime())
                .build();


        this.merchantMovementEmitter.send(Message.of(onlineMovement));

        return Response.ok(new CreateMovementResponseDTO(onlineMovement.getId())).status(201).build();
    }
}