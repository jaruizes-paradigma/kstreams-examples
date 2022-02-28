package com.jaruizes.kstreams.examples.fraudsimulator.api.async.serializers;


import com.jaruizes.kstreams.examples.fraudsimulator.adapters.eventbus.dto.SimulationDataDTO;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class SimulationDataDTODeserializer extends ObjectMapperDeserializer<SimulationDataDTO> {
    public SimulationDataDTODeserializer() {
        super(SimulationDataDTO.class);
    }
}