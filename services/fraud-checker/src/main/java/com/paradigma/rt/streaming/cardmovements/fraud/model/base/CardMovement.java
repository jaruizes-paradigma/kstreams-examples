package com.paradigma.rt.streaming.cardmovements.fraud.model.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.paradigma.rt.streaming.cardmovements.fraud.model.input.ATMMovement;
import com.paradigma.rt.streaming.cardmovements.fraud.model.input.MerchantMovement;
import com.paradigma.rt.streaming.cardmovements.fraud.model.input.OnlineMovement;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.WRAPPER_OBJECT, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value= ATMMovement.class),
        @JsonSubTypes.Type(value= MerchantMovement.class),
        @JsonSubTypes.Type(value= OnlineMovement.class)
})
public interface CardMovement {}
