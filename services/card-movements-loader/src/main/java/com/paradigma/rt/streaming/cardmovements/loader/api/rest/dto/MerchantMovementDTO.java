package com.paradigma.rt.streaming.cardmovements.loader.api.rest.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MerchantMovementDTO extends CardMovementDTO {

    private String merchant;
}
