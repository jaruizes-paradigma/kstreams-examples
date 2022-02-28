package com.jaruizes.kstreams.examples.fraudsimulator.business.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CardFraudConfig {

    private String card;
    private List<Integer> fraudTypes;
}
