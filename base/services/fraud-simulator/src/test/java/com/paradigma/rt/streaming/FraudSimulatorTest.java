package com.paradigma.rt.streaming;

import com.paradigma.rt.streaming.fraudsimulator.adapters.eventbus.dto.*;
import com.paradigma.rt.streaming.fraudsimulator.api.rest.dto.StartFraudSimulationDTO;
import com.paradigma.rt.streaming.fraudsimulator.api.rest.dto.StartFraudSimulationResponseDTO;
import com.paradigma.rt.streaming.fraudsimulator.business.FraudSimulator;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(KafkaResource.class)
public class FraudSimulatorTest {

    @ConfigProperty(name = "mp.messaging.outgoing.atm-movements-out.topic")
    String atmMovementsTopic;

    @ConfigProperty(name = "mp.messaging.outgoing.online-movements-out.topic")
    String onlineMovementsTopic;

    @ConfigProperty(name = "mp.messaging.outgoing.merchant-movements-out.topic")
    String merchantMovementsTopic;

    @ConfigProperty(name = "mp.messaging.incoming.datagen-fraudsimulator-in.topic")
    String fraudSimulatorInTopic;

    @ConfigProperty(name = "mp.messaging.outgoing.datagen-fraudsimulator-results.topic")
    String fraudSimulatorResultsTopic;

    @Inject
    FraudSimulator fraudSimulator;

    KafkaConsumer<String, ATMMovementDTO> atmConsumer;
    KafkaConsumer<String, OnlineMovementDTO> onlineConsumer;
    KafkaConsumer<String, MerchantMovementDTO> merchantConsumer;
    KafkaConsumer<String, SimulationDataDTO> fraudSimulatorConsumer;
    KafkaConsumer<String, SimulationDataResultsDTO> fraudSimulatorResultsConsumer;

    @BeforeEach
    public void setUp(){
        atmConsumer = new KafkaConsumer(consumerProps("atm"), new StringDeserializer(), new ObjectMapperDeserializer<>(ATMMovementDTO.class));
        onlineConsumer = new KafkaConsumer(consumerProps("online"), new StringDeserializer(), new ObjectMapperDeserializer<>(OnlineMovementDTO.class));
        merchantConsumer = new KafkaConsumer(consumerProps("merchant"), new StringDeserializer(), new ObjectMapperDeserializer<>(MerchantMovementDTO.class));
        fraudSimulatorConsumer = new KafkaConsumer(consumerProps("data-gen"), new StringDeserializer(), new ObjectMapperDeserializer<>(SimulationDataDTO.class));
        fraudSimulatorResultsConsumer = new KafkaConsumer(consumerProps("data-gen-results"), new StringDeserializer(), new ObjectMapperDeserializer<>(SimulationDataResultsDTO.class));

        fraudSimulatorConsumer.subscribe(Collections.singletonList(fraudSimulatorInTopic));
        fraudSimulatorResultsConsumer.subscribe(Collections.singletonList(fraudSimulatorResultsTopic));
        atmConsumer.subscribe(Collections.singletonList(atmMovementsTopic));
        onlineConsumer.subscribe(Collections.singletonList(onlineMovementsTopic));
        merchantConsumer.subscribe(Collections.singletonList(merchantMovementsTopic));
    }

    @AfterEach
    public void tearDown(){
        atmConsumer.close();
        onlineConsumer.close();
        merchantConsumer.close();
        fraudSimulatorConsumer.close();
        fraudSimulatorResultsConsumer.close();
    }

    @Test
    public void testSimulator() {
        final StartFraudSimulationDTO startFraudSimulationDTO = StartFraudSimulationDTO.builder()
                .cards(4)
                .iterations(2)
                .msBetweenIterations(10000)
                .build();

        StartFraudSimulationResponseDTO response =
                given()
                    .body(startFraudSimulationDTO)
                    .contentType("application/json")
                .when()
                    .post("/fraud-process-simulation")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(StartFraudSimulationResponseDTO.class);
        // Then
        // Id should be generated and Id is greater than 0
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.getId() > 0);

        // There should be exactly one message in topic associated to simulation data input
        ConsumerRecords<String, SimulationDataDTO> simulationData = fraudSimulatorConsumer.poll(Duration.ofSeconds(10));
        Assertions.assertEquals(1, simulationData.count());

        final SimulationDataDTO simulationDataDTO = simulationData.iterator().next().value();
        // Id should be equals to response id
        Assertions.assertEquals(response.getId(), simulationDataDTO.getId());

        // Cards, iterations and time between iterations should be equals to the initial object (POST request)
        Assertions.assertEquals(startFraudSimulationDTO.getCards(), simulationDataDTO.getCards());
        Assertions.assertEquals(startFraudSimulationDTO.getIterations(), simulationDataDTO.getIterations());
        Assertions.assertEquals(startFraudSimulationDTO.getMsBetweenIterations(), simulationDataDTO.getMsBetweenIterations());

        ConsumerRecords<String, SimulationDataResultsDTO> simulationDataResultsRecords = fraudSimulatorResultsConsumer.poll(Duration.ofSeconds(60));

        // There should be exactly one message in topic associated to simulation data results
        Assertions.assertEquals(1, simulationDataResultsRecords.count());

        final SimulationDataResultsDTO resultsDTO = simulationDataResultsRecords.iterator().next().value();
        // Id should be equals to response id
        Assertions.assertEquals(response.getId(), resultsDTO.getId());

        // Cards, iterations and time between iterations should be equals to the initial object (POST request)
        Assertions.assertEquals(startFraudSimulationDTO.getCards(), resultsDTO.getCards());
        Assertions.assertEquals(startFraudSimulationDTO.getIterations(), resultsDTO.getIterations());
        Assertions.assertEquals(startFraudSimulationDTO.getMsBetweenIterations(), resultsDTO.getMsBetweenIterations());
        Assertions.assertEquals(FraudSimulator.calculatePotentialFraudsMovements(resultsDTO.getCards(), resultsDTO.getIterations()), resultsDTO.getPotentialFraudMovements());

        ConsumerRecords<String, ATMMovementDTO> atmMovements = atmConsumer.poll(Duration.ofSeconds(60));
        ConsumerRecords<String, OnlineMovementDTO> onlineMovements = onlineConsumer.poll(Duration.ofSeconds(60));
        ConsumerRecords<String, MerchantMovementDTO> merchantMovements = merchantConsumer.poll(Duration.ofSeconds(60));
        int totalMessagesInMovementsTopics = atmMovements.count() + onlineMovements.count() + merchantMovements.count();
        Assertions.assertEquals(totalMessagesInMovementsTopics, resultsDTO.getMovementsGenerated());


    }

    private Properties consumerProps(String groupId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaResource.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    private Properties adminProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaResource.getBootstrapServers());
        return props;
    }
}