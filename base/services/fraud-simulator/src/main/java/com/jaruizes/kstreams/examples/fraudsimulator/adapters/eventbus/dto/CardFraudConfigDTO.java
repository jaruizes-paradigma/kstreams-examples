package com.jaruizes.kstreams.examples.fraudsimulator.adapters.eventbus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CardFraudConfigDTO {

    private String card;
    private List<Integer> fraudTypes;
}
