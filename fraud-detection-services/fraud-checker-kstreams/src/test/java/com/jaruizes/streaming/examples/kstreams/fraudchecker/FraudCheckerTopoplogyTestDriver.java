package com.jaruizes.streaming.examples.kstreams.fraudchecker;

import com.jaruizes.streaming.examples.kstreams.fraudchecker.dto.ATMMovementDTO;
import com.jaruizes.streaming.examples.kstreams.fraudchecker.model.CardMovement;
import com.jaruizes.streaming.examples.kstreams.fraudchecker.model.PotentialFraudCase;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.test.TestRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;


@QuarkusTest
public class FraudCheckerTopoplogyTestDriver {

    public static final String CARD_1 = "card1";
    public static final String CARD_2 = "card2";
    @Inject
    Topology topology;

    @ConfigProperty(name = "topics.input.atm-movements")
    String atmMovementsTopicName;

    @ConfigProperty(name = "topics.output.fraud-movements")
    String fraudOutputTopicName;

    TopologyTestDriver testDriver;

    TestInputTopic<String, ATMMovementDTO> atmMovementsTopic;

    TestOutputTopic<String, PotentialFraudCase> fraudMovementsTopic;

    @BeforeEach
    public void setUp(){
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "testApplicationId");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        testDriver = new TopologyTestDriver(topology, config);

        atmMovementsTopic = testDriver.createInputTopic(atmMovementsTopicName, new StringSerializer(), new ObjectMapperSerializer());

        fraudMovementsTopic = testDriver.createOutputTopic(fraudOutputTopicName, new StringDeserializer(), new ObjectMapperDeserializer<>(PotentialFraudCase.class));
    }

    @AfterEach
    public void tearDown(){
        testDriver.close();
    }

    @Test
    public void everyATMMovement_isCorrect_noATMFraudCase(){
        Instant now = Instant.now();

        final List<ATMMovementDTO> atmMovements = Arrays.asList(
                atmMovementBuilder("atm1", CARD_1, 20.5f, now),
                atmMovementBuilder("atm1", CARD_2, 10.5f, now.plusSeconds(5)),
                atmMovementBuilder("atm1", CARD_1, 200, now.plusSeconds(10)),
                atmMovementBuilder("atm1", CARD_1, 200, now.plusSeconds(15)));

        atmMovementsTopic.pipeInput(atmMovementBuilder("xx", "xx", 200, now.plusSeconds(100)));
        atmMovementsTopic.pipeValueList(atmMovements);

        Assertions.assertTrue(fraudMovementsTopic.isEmpty());
    }

    @Test
    public void simple_ATMFraudCase(){
        Instant now = Instant.now();
        final List<ATMMovementDTO> atmMovements = Arrays.asList(
            atmMovementBuilder("atm1", CARD_1, 100, now),                          // No fraud
            atmMovementBuilder("atm2", CARD_2, 100, now.plusSeconds(5)),           // No fraud
            atmMovementBuilder("atm1", CARD_1, 120, now.plusSeconds(10)),          // No fraud
            atmMovementBuilder("atm1", CARD_1, 120, now.plusSeconds(15)),          // No fraud
            atmMovementBuilder("atm2", CARD_1, 150, now.plusSeconds(20)),          // Fraud
            atmMovementBuilder("xx", "xx", 150, now.plusSeconds(100)));

        atmMovementsTopic.pipeValueList(atmMovements);

        TestRecord<String, PotentialFraudCase> result = fraudMovementsTopic.readRecord();

        Assertions.assertNotNull(result);
        PotentialFraudCase fraudCase = result.getValue();
        Assertions.assertNotNull(fraudCase);
        Assertions.assertEquals(FraudCheckerTopology.MULTIPLE_ATM_MOVEMENTS_IN_SHORT_PERIOD, fraudCase.getType());

        List<CardMovement> cardMovements = fraudCase.getMovements();
        Assertions.assertNotNull(cardMovements);
        Assertions.assertEquals(4, cardMovements.size());
        checkMovements(cardMovements, CARD_1, "atm");
    }

    @Test
    public void simple_twoFraudCases_withTwoCards(){
        atmMovementsTopic.pipeValueList(buildATMMovements(Instant.now(), 60));

        checkFraudCases(1);
    }

    @Test
    public void multipleSessions_FraudCases_withTwoCards(){
        Instant now = Instant.now();
        int iterations = 10;

        for (int i=0; i<iterations; i++) {
            atmMovementsTopic.pipeValueList(buildATMMovements(now, 60));
            now = now.plusSeconds(200 * (i+1));
        }

        checkFraudCases(iterations);
    }

    private List<ATMMovementDTO> buildATMMovements(Instant initialTime, int sessionDuration) {
        return Arrays.asList(
                atmMovementBuilder("atm1-1", CARD_1, 100, initialTime),                         // No fraud
                atmMovementBuilder("atm2-1", CARD_2, 100, initialTime.plusSeconds(5)),          // No fraud
                atmMovementBuilder("atm1-1", CARD_1, 200, initialTime.plusSeconds(10)),         // No fraud
                atmMovementBuilder("atm2-1", CARD_2, 201, initialTime.plusSeconds(15)),         // No fraud
                atmMovementBuilder("atm2-1", CARD_2, 210, initialTime.plusSeconds(20)),         // No fraud
                atmMovementBuilder("atm1-1", CARD_1, 201, initialTime.plusSeconds(25)),         // No fraud
                atmMovementBuilder("atm2-1", CARD_2, 220, initialTime.plusSeconds(35)),         // No fraud
                atmMovementBuilder("atm2-1", CARD_2, 230, initialTime.plusSeconds(40)),         // No fraud
                atmMovementBuilder("atm2-2", CARD_2, 250, initialTime.plusSeconds(50)),         // Fraud Card 2
                atmMovementBuilder("atm1-2", CARD_1, 250, initialTime.plusSeconds(55)),         // Fraud Card 1
                atmMovementBuilder("xx", "xx", 200, initialTime.plusSeconds(55 + sessionDuration)));
    }

    private void checkFraudCases(int iterations) {
        List<PotentialFraudCase> fraudCases = fraudMovementsTopic.readValuesToList();

        Assertions.assertNotNull(fraudCases);
        Assertions.assertEquals(iterations * 2, fraudCases.size());

        fraudCases.forEach((fraudCase) -> {
            Assertions.assertEquals(FraudCheckerTopology.MULTIPLE_ATM_MOVEMENTS_IN_SHORT_PERIOD, fraudCase.getType());

            if (fraudCase.getCard().equalsIgnoreCase(CARD_1)) {
                Assertions.assertEquals(4, fraudCase.getMovements().size());
                checkMovements(fraudCase.getMovements(), CARD_1, "atm1-");
            } else if (fraudCase.getCard().equalsIgnoreCase(CARD_2)) {
                Assertions.assertEquals(6, fraudCase.getMovements().size());
                checkMovements(fraudCase.getMovements(), CARD_2, "atm2-");
            } else {
                Assertions.fail("Cards should be only card1 or card2");
            }
        });
    }

    private void checkMovements(List<CardMovement> movements, String card, String prefix) {
        movements.forEach((mov) -> {
            Assertions.assertTrue(mov.getOrigin().startsWith(prefix));
            Assertions.assertEquals(card, mov.getCard());
        });
    }

    private ATMMovementDTO atmMovementBuilder(String atm, String card, float amount, Instant createdAT) {
        return ATMMovementDTO.builder()
                .atm(atm)
                .card(card)
                .amount(amount)
                .createdAt(Date.from(createdAT))
                .build();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}