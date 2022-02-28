package com.jaruizes.kstreams.examples.fraudsimulator.api.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CardFraudConfigDTO {

    private String card;
    private List<Integer> fraudTypes;
}
