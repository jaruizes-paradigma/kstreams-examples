package com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud;

import com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.model.base.CardMovement;
import com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.model.input.ATMMovement;
import com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud.model.output.FraudMovements;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.test.TestRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.*;

import javax.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.*;


@QuarkusTest
public class FraudCheckerTopoplogyTestDriver {

    @Inject
    Topology topology;

    @ConfigProperty(name = "topics.input.atm-movements")
    String atmMovementsTopicName;

    @ConfigProperty(name = "topics.output.fraud-movements")
    String fraudOutputTopicName;

    TopologyTestDriver testDriver;

    TestInputTopic<String, ATMMovement> atmMovementsTopic;

    TestOutputTopic<String, FraudMovements> fraudMovementsTopic;

    @BeforeEach
    public void setUp(){
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "testApplicationId");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        testDriver = new TopologyTestDriver(topology, config);

        atmMovementsTopic = testDriver.createInputTopic(atmMovementsTopicName, new StringSerializer(), new ObjectMapperSerializer());

        fraudMovementsTopic = testDriver.createOutputTopic(fraudOutputTopicName, new StringDeserializer(), new ObjectMapperDeserializer<>(FraudMovements.class));
    }

    @AfterEach
    public void tearDown(){
        testDriver.close();
    }

    @Test
    @Timeout(value = 70)
    public void when_MultipleATMMovementsAreGeneratedInDifferentATMs_Then_AFraudMovementContainingThoseMovementsIsPublishedToKafka(){
        final List<ATMMovement> atmMovements = new ArrayList<>();
        atmMovements.add(atmMovementBuilder("atm1", "card1", 20.5f));
        atmMovements.add(atmMovementBuilder("atm2", "card2", 10.5f));
        atmMovements.add(atmMovementBuilder("atm2", "card1", 200));

        atmMovementsTopic.pipeValueList(atmMovements, Instant.now(), Duration.ofSeconds(5));

        TestRecord<String, FraudMovements> result = fraudMovementsTopic.readRecord();

        Assertions.assertNotNull(result.getValue());
        Assertions.assertNotNull(result.getValue().getCardMovements());
        Assertions.assertEquals(FraudCheckerTopology.MULTIPLE_ATM_MOVEMENTS_IN_SHORT_PERIOD, result.getValue().getType());

        CardMovement[] cardMovements = (CardMovement[]) result.getValue().getCardMovements().toArray();
        Assertions.assertEquals(2, cardMovements.length);
        ATMMovement atmMovement1 = (ATMMovement) cardMovements[0];
        ATMMovement atmMovement2 = (ATMMovement) cardMovements[1];

        Assertions.assertEquals("atm1", atmMovement1.getAtm());
        Assertions.assertEquals("card1", atmMovement1.getCard());
        Assertions.assertEquals("atm2", atmMovement2.getAtm());
        Assertions.assertEquals("card1", atmMovement2.getCard());


    }


    private ATMMovement atmMovementBuilder(String atm, String card, float amount) {
        return ATMMovement.builder()
                .atm(atm)
                .card(card)
                .amount(amount)
                .build();
    }

}