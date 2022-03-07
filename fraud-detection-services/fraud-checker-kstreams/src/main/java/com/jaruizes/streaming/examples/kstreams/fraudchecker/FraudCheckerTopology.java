package com.jaruizes.streaming.examples.kstreams.fraudchecker;

import com.jaruizes.streaming.examples.kstreams.fraudchecker.business.CheckATMFraud;
import com.jaruizes.streaming.examples.kstreams.fraudchecker.dto.ATMMovementDTO;
import com.jaruizes.streaming.examples.kstreams.fraudchecker.model.CardMovement;
import com.jaruizes.streaming.examples.kstreams.fraudchecker.model.PotentialFraudCase;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class FraudCheckerTopology {

    public final static int MULTIPLE_ATM_MOVEMENTS_IN_SHORT_PERIOD = 1;
    public final static int ATM_AND_MERCHANT_IN_SHORT_PERIOD = 2;
    public final static int MULTIPLE_MERCHANT_MOVEMENTS_IN_SHORT_PERIOD = 3;
    public final static int MULTIPLE_ONLINE_MOVEMENTS_IN_SHORT_PERIOD = 4;
    public static final int WINDOW_DURATION = 60;

    @Inject
    private CheckATMFraud checkATMFraud;

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

        this.checkATMFraud.check(builder);

//        Serde<ATMMovementDTO> atmMovementSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(ATMMovementDTO.class));
//        Serde<CardMovement> cardMovementSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(CardMovement.class));
//        Serde<PotentialFraudCase> potentialFraudCaseSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(PotentialFraudCase.class));
//
//        KStream<String, ATMMovementDTO> atmStreamSource = builder.stream(atmMovementsTopic, Consumed.with(Serdes.String(), atmMovementSerde).withTimestampExtractor(new CardMovementTimestampExtractor()));
//
//        atmStreamSource
//                .selectKey((k,v) -> v.getCard())
//                .mapValues(this::buildFromDTO)
//                .groupByKey(Grouped.with(Serdes.String(), cardMovementSerde))
//                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(WINDOW_DURATION)))
//                .aggregate(() -> new PotentialATMFraudCase(),
//                        (key, atmMovement, potentialFraudCase) -> potentialATMFraudCaseAggregate(key, atmMovement, potentialFraudCase),
//                        Materialized.with(Serdes.String(), potentialFraudCaseSerde))
//                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded().shutDownWhenFull()))
//                .toStream()
//                .filter((k, v) -> v.isFraud())
//                .map((windowedKey, value) -> KeyValue.pair(windowedKey.key(), value))
//                .to(fraudOutputTopic, Produced.with(Serdes.String(), potentialFraudCaseSerde));


        return builder.build();

    }


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

    private PotentialFraudCase potentialATMFraudCaseAggregate(String key, CardMovement atmMovement, PotentialFraudCase potentialFraudCase) {
        potentialFraudCase.setCard(atmMovement.getCard());
        potentialFraudCase.addMovement(atmMovement);

        return potentialFraudCase;
    }
}