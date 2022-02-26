package com.paradigma.rt.streaming.fraudsimulator.api.async.serializers;


import com.paradigma.rt.streaming.fraudsimulator.api.rest.dto.StartFraudSimulationDTO;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class StartFraudSimulationDTODeserializer extends ObjectMapperDeserializer<StartFraudSimulationDTO> {
    public StartFraudSimulationDTODeserializer() {
        super(StartFraudSimulationDTO.class);
    }
}