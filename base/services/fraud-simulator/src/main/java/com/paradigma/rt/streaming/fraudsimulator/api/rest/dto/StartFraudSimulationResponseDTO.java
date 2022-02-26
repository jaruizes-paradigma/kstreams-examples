package com.paradigma.rt.streaming.fraudsimulator.api.rest.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StartFraudSimulationResponseDTO {

    private long id;

    public StartFraudSimulationResponseDTO(long id) {
        this.id = id;
    }
}
