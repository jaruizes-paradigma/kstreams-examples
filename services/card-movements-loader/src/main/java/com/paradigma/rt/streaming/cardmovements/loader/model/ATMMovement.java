package com.paradigma.rt.streaming.cardmovements.loader.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
public class ATMMovement extends CardMovement {

    private String atm;
}
