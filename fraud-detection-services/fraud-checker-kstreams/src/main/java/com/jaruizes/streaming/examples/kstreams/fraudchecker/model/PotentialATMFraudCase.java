package com.jaruizes.streaming.examples.kstreams.fraudchecker.model;

import com.jaruizes.streaming.examples.kstreams.fraudchecker.FraudCheckerTopology;
import lombok.Data;


@Data
public class PotentialATMFraudCase extends PotentialFraudCase{

    public PotentialATMFraudCase() {
        super();
        setType(FraudCheckerTopology.MULTIPLE_ATM_MOVEMENTS_IN_SHORT_PERIOD);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
