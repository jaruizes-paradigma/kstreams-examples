package com.paradigma.rt.streaming.cardmovements.loader.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class OnlineMovement extends CardMovement {

    private String site;
}
