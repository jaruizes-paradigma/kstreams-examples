package com.paradigma.rt.streaming.cardmovements.fraud.transformers;

import com.paradigma.rt.streaming.cardmovements.fraud.model.input.ATMMovement;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

public class ATMMovementTransformer implements ValueTransformerWithKey<String, ATMMovement, ATMMovement> {
    private KeyValueStore<String, ATMMovement> state;
    private String stateStoreName;

    public ATMMovementTransformer(String stateStoreName) {
        this.stateStoreName = stateStoreName;
    }

    @Override
    public void init(ProcessorContext processorContext) {
        state = processorContext.getStateStore(this.stateStoreName);
    }

    @Override
    public ATMMovement transform(String card, ATMMovement currentATMMovement) {
        ATMMovement prevATMMovement = state.get(card);

        state.put(card, currentATMMovement);
        if (prevATMMovement != null) {
            String prevATM = prevATMMovement.getAtm();
            String currentATM = currentATMMovement.getAtm();
            if (!prevATM.equalsIgnoreCase(currentATM)) {
                return currentATMMovement;
            } else {
                return null;
            }
        }

        return currentATMMovement;
    }

    @Override
    public void close() {}
}