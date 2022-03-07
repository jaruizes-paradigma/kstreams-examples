package com.jaruizes.streaming.examples.kstreams.fraudchecker.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

@Data
@SuperBuilder
@NoArgsConstructor
public class ATMMovementDTO extends CardMovementDTO {

    private String atm;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CardMovementDTO)) return false;
        ATMMovementDTO that = (ATMMovementDTO) o;
        return this.getAtm().equals(that.getAtm()) && this.getCard().equals(that.getCard());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getCard(), this.getAtm());
    }
}
