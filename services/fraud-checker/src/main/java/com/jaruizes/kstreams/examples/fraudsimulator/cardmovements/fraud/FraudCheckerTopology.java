package com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud;

import com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.model.input.ATMMovement;
import com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.model.input.MerchantMovement;
import com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.model.input.OnlineMovement;
import com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.model.output.FraudMovements;
import com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.transformers.ATMMovementTransformer;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.time.Duration;

@ApplicationScoped
public class FraudCheckerTopology {

    public final static int MULTIPLE_ATM_MOVEMENTS_IN_SHORT_PERIOD = 1;
    public final static int ATM_AND_MERCHANT_IN_SHORT_PERIOD = 2;
    public final static int MULTIPLE_MERCHANT_MOVEMENTS_IN_SHORT_PERIOD = 3;
    public final static int MULTIPLE_ONLINE_MOVEMENTS_IN_SHORT_PERIOD = 4;

    @ConfigProperty(name = "topics.input.atm-movements")
    String atmMovementsTopic;

    @ConfigProperty(name = "topics.input.online-movements")
    String onlineMovementsTopic;

    @ConfigProperty(name = "topics.input.merchant-movements")
    String merchantMovementsTopic;

    @ConfigProperty(name = "topics.output.fraud-movements")
    String fraudOutputTopic;

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        Serde<ATMMovement> atmMovementSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(ATMMovement.class));
        Serde<OnlineMovement> onlineMovementSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(OnlineMovement.class));
        Serde<MerchantMovement> merchantMovementSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(MerchantMovement.class));
        Serde<FraudMovements> fraudMovementsSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(FraudMovements.class));

        // Movimientos de cajero. Seleccionamos como clave el número de tarjeta
        KStream<String, ATMMovement> atmStream = builder.stream(atmMovementsTopic, Consumed.with(Serdes.String(), atmMovementSerde))
                .selectKey((k,v) -> v.getCard());

        StoreBuilder<KeyValueStore<String, ATMMovement>> atmMovementsStore = Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore("atm-movements"),
                Serdes.String(),
                atmMovementSerde);
        builder.addStateStore(atmMovementsStore);

        atmStream.transformValues(() -> new ATMMovementTransformer("atm-movements"), "atm-movements")
                .filter((k,v) -> v != null)
                .groupByKey(Grouped.with(Serdes.String(), atmMovementSerde))
                .windowedBy(TimeWindows.of(Duration.ofSeconds(60)))
                .aggregate(() -> new FraudMovements(MULTIPLE_ATM_MOVEMENTS_IN_SHORT_PERIOD),
                        (key, value, aggregate) -> {
                            aggregate.setCard(value.getCard());
                            aggregate.addMovement(value);

                            return aggregate;
                    }, Materialized.with(Serdes.String(), fraudMovementsSerde))
                .filter((k,v) -> v.getCardMovements().size() >= 2)
                .toStream()
                .map((windowedKey, value) -> KeyValue.pair(value.getCard(), value))
                .to(fraudOutputTopic, Produced.with(Serdes.String(), fraudMovementsSerde));







//        KStream<String, OnlineMovement> onlineStream = builder.stream(onlineMovementsTopic, Consumed.with(Serdes.String(), onlineMovementSerde));
//        KStream<String, MerchantMovement> merchantStream = builder.stream(merchantMovementsTopic, Consumed.with(Serdes.String(), merchantMovementSerde));

//        atmStream.filter((k,v) -> v.getAtm().equalsIgnoreCase("test1")).to(fraudOutputTopic, Produced.with(Serdes.String(), atmMovementSerde));

        return builder.build();

    }
}