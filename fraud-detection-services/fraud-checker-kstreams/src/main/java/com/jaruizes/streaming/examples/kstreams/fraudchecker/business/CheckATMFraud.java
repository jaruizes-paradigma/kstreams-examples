package com.jaruizes.streaming.examples.kstreams.fraudchecker.business;

import com.jaruizes.streaming.examples.kstreams.fraudchecker.dto.ATMMovementDTO;
import com.jaruizes.streaming.examples.kstreams.fraudchecker.extractors.CardMovementTimestampExtractor;
import com.jaruizes.streaming.examples.kstreams.fraudchecker.model.CardMovement;
import com.jaruizes.streaming.examples.kstreams.fraudchecker.model.PotentialATMFraudCase;
import com.jaruizes.streaming.examples.kstreams.fraudchecker.model.PotentialFraudCase;
import com.jaruizes.streaming.examples.kstreams.fraudchecker.transformers.ATMTransformer;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.time.Duration;

@ApplicationScoped
public class CheckATMFraud {
    public static final int WINDOW_DURATION = 60;

    @ConfigProperty(name = "topics.input.atm-movements")
    String atmMovementsTopic;

    @ConfigProperty(name = "topics.output.fraud-movements")
    String fraudOutputTopic;

    public void check(StreamsBuilder builder) {
        Serde<ATMMovementDTO> atmMovementSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(ATMMovementDTO.class));
        Serde<CardMovement> cardMovementSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(CardMovement.class));
        Serde<PotentialFraudCase> potentialFraudCaseSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(PotentialFraudCase.class));

//        WindowBytesStoreSupplier atmWindowStore = Stores.persistentWindowStore("atm-movements", Duration.ofSeconds(WINDOW_DURATION), Duration.ofSeconds(WINDOW_DURATION), false);
//        final StoreBuilder<WindowStore<String, CardMovement>> atmStore = Stores.windowStoreBuilder(atmWindowStore, Serdes.String(), cardMovementSerde);
//        builder.addStateStore(atmStore);

        final StoreBuilder<KeyValueStore<String, CardMovement>> atmStore = Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore("atm-movements"), Serdes.String(), cardMovementSerde);
        builder.addStateStore(atmStore);

        KStream<String, ATMMovementDTO> atmStreamSource = builder.stream(atmMovementsTopic, Consumed.with(Serdes.String(), atmMovementSerde).withTimestampExtractor(new CardMovementTimestampExtractor()));
        SessionWindows sessionWindow = SessionWindows.ofInactivityGapWithNoGrace(Duration.ofSeconds(WINDOW_DURATION));

        atmStreamSource
                .selectKey((k,v) -> v.getCard())
                .transform(ATMTransformer::new, "atm-movements")
                .groupByKey(Grouped.with(Serdes.String(), cardMovementSerde))
                .windowedBy(sessionWindow)
                .aggregate(PotentialATMFraudCase::new,
                        (key, atmMovement, potentialFraudCase) -> potentialATMFraudCaseAggregate(key, atmMovement, potentialFraudCase),
                        (k, a, b) -> simpleMerge(a, b),
                        Materialized.with(Serdes.String(), potentialFraudCaseSerde))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded().shutDownWhenFull()))
                .toStream()
                .filter((k,v) -> v != null && v.isFraud())
                .map((windowedKey, value) -> KeyValue.pair(windowedKey.key(), value))
                .to(fraudOutputTopic, Produced.with(Serdes.String(), potentialFraudCaseSerde));
    }

    private PotentialFraudCase simpleMerge(PotentialFraudCase a, PotentialFraudCase b) {
        return b;
    }

    private PotentialFraudCase potentialATMFraudCaseAggregate(String key, CardMovement atmMovement, PotentialFraudCase potentialFraudCaseAggregate) {
        potentialFraudCaseAggregate.setCard(atmMovement.getCard());
        potentialFraudCaseAggregate.addMovement(atmMovement);
        if (atmMovement.isFraud()) {
            potentialFraudCaseAggregate.setFraud(true);
        }

        return potentialFraudCaseAggregate;
    }
}
