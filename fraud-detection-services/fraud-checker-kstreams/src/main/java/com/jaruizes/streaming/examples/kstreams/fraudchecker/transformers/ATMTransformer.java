package com.jaruizes.streaming.examples.kstreams.fraudchecker.transformers;

import com.jaruizes.streaming.examples.kstreams.fraudchecker.dto.ATMMovementDTO;
import com.jaruizes.streaming.examples.kstreams.fraudchecker.model.CardMovement;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

public class ATMTransformer implements Transformer<String, ATMMovementDTO, KeyValue<String, CardMovement>> {
    private KeyValueStore<String, CardMovement> state;
    private String stateStoreName;

    public ATMTransformer() {
        this.stateStoreName = "atm-movements";
    }

    @Override
    public void init(ProcessorContext processorContext) {
        state = processorContext.getStateStore(this.stateStoreName);
    }

    @Override
    public KeyValue<String, CardMovement> transform(String card, ATMMovementDTO currentATMMovementDTO) {
        CardMovement lastCardMovement = state.get(card);
        CardMovement currentCardMovement = buildFromDTO(currentATMMovementDTO);

        state.put(card, currentCardMovement);

        if (lastCardMovement != null && !lastCardMovement.getOrigin().equalsIgnoreCase(currentCardMovement.getOrigin())) {
            System.out.println("---> TRANSFORM - card: " + currentCardMovement.getCard() + " / ATM: " + currentCardMovement.getOrigin() + " ---- Fraud detected");
            currentCardMovement.setFraud(true);

            return KeyValue.pair(card, currentCardMovement);
        }

        return KeyValue.pair(card, currentCardMovement);
    }

//    @Override
//    public CardMovement transform(String card, ATMMovementDTO currentATMMovementDTO) {
//        CardMovement lastCardMovement = state.get(card);
//        CardMovement currentCardMovement = buildFromDTO(currentATMMovementDTO);
//
//        state.put(card, currentCardMovement);
//
//        if (lastCardMovement != null && !lastCardMovement.getOrigin().equalsIgnoreCase(currentCardMovement.getOrigin())) {
//            System.out.println("---> TRANSFORM - card: " + currentATMMovementDTO.getCard() + " / ATM: " + currentATMMovementDTO.getAtm() + " ---- Fraud detected");
//            currentCardMovement.setFraud(true);
//
//            return currentCardMovement;
//        }
//
//        return currentCardMovement;
//    }

    @Override
    public void close() {}

    private CardMovement buildFromDTO(ATMMovementDTO dto) {
        return CardMovement.builder()
                .amount(dto.getAmount())
                .card(dto.getCard())
                .id(dto.getId())
                .createdAt(dto.getCreatedAt())
                .origin(dto.getAtm())
                .type(1)
                .build();
    }
}
