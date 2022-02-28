package com.jaruizes.kstreams.examples.fraudsimulator;

import com.jaruizes.kstreams.examples.fraudsimulator.adapters.eventbus.dto.*;
import com.jaruizes.kstreams.examples.fraudsimulator.api.rest.dto.CardFraudConfigDTO;
import com.jaruizes.kstreams.examples.fraudsimulator.api.rest.dto.StartFraudSimulationDTO;
import com.jaruizes.kstreams.examples.fraudsimulator.api.rest.dto.StartFraudSimulationResponseDTO;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    @Timeout(value = 60)
    public void testCase_AllFraudCases() {
        final SimulationDataResultsDTO resultsDTO = baseTest(Arrays.asList(1,2,3,4));
        this.checkMovementsCaseAll(resultsDTO);
    }

    @Test
    @Timeout(value = 60)
    public void testCase_MultipleATMMovements() {
        final SimulationDataResultsDTO resultsDTO = baseTest(Arrays.asList(1));
        this.checkMovementsCaseMultipleATM(resultsDTO);
    }

    @Test
    @Timeout(value = 60)
    public void testCase_MultipleMerchantMovements() {
        final SimulationDataResultsDTO resultsDTO = baseTest(Arrays.asList(2));
        this.checkMovementsCaseMultipleMerchant(resultsDTO);
    }

    @Test
    @Timeout(value = 60)
    public void testCase_MultipleOnlineMovements() {
        final SimulationDataResultsDTO resultsDTO = baseTest(Arrays.asList(3));
        this.checkMovementsCaseMultipleOnline(resultsDTO);
    }

    @Test
    @Timeout(value = 60)
    public void testCase_PotentialOnlineFraudSite() {
        final SimulationDataResultsDTO resultsDTO = baseTest(Arrays.asList(4));
        this.checkMovementsCaseOnlinePotentialFraudSite(resultsDTO);
    }

    @Test
    @Timeout(value = 60)
    public void testCase_ATMAndMerchantMovements() {
        final SimulationDataResultsDTO resultsDTO = baseTest(Arrays.asList(5));
        this.checkMovementsCaseATMAndMerchant(resultsDTO);
    }

    private SimulationDataResultsDTO baseTest(List<Integer> fraudTypes) {
        final StartFraudSimulationDTO startFraudSimulationDTO = this.buildRequestObject(fraudTypes);
        final StartFraudSimulationResponseDTO response = this.callRestAPItoStartFraudSimulatorProcess(startFraudSimulationDTO);

        // Id should be generated and Id is greater than 0
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.getId() > 0);

        // Checking topics to validate that everything is generated
        this.checkDataInputTopic(startFraudSimulationDTO, response.getId());
        return this.checkResultsTopic(startFraudSimulationDTO, response.getId());
    }

    private StartFraudSimulationDTO buildRequestObject(List<Integer> fraudTypes) {
        return StartFraudSimulationDTO.builder()
                .iterations(2)
                .msBetweenIterations(10000)
                .cardsFraudConfig(
                        IntStream.range(0,1)
                        .mapToObj((index) -> CardFraudConfigDTO.builder()
                                        .card("card-" + index)
                                        .fraudTypes(fraudTypes)
                                        .build())
                        .collect(Collectors.toList())
                    )
                .build();
    }

    private StartFraudSimulationResponseDTO callRestAPItoStartFraudSimulatorProcess(StartFraudSimulationDTO startFraudSimulationDTO) {
        return given()
                .body(startFraudSimulationDTO)
                .contentType("application/json")
                .when()
                .post("/fraud-simulation")
                .then()
                .statusCode(201)
                .extract()
                .as(StartFraudSimulationResponseDTO.class);
    }

    private SimulationDataDTO checkDataInputTopic(StartFraudSimulationDTO startFraudSimulationDTO, long processId) {
        // There should be exactly one message in topic associated to simulation data input
        ConsumerRecords<String, SimulationDataDTO> simulationData = fraudSimulatorConsumer.poll(Duration.ofSeconds(10));
        Assertions.assertEquals(1, simulationData.count());

        final SimulationDataDTO simulationDataDTO = simulationData.iterator().next().value();
        // Id should be equals to response id
        Assertions.assertEquals(processId, simulationDataDTO.getId());

        // Iterations and time between iterations should be equals to the initial object (POST request)
        Assertions.assertEquals(startFraudSimulationDTO.getIterations(), simulationDataDTO.getIterations());
        Assertions.assertEquals(startFraudSimulationDTO.getMsBetweenIterations(), simulationDataDTO.getMsBetweenIterations());

        return simulationDataDTO;
    }

    private SimulationDataResultsDTO checkResultsTopic(StartFraudSimulationDTO startFraudSimulationDTO, long processId) {
        ConsumerRecords<String, SimulationDataResultsDTO> simulationDataResultsRecords = fraudSimulatorResultsConsumer.poll(Duration.ofSeconds(60));

        // There should be exactly one message in topic associated to simulation data results
        Assertions.assertEquals(1, simulationDataResultsRecords.count());

        final SimulationDataResultsDTO resultsDTO = simulationDataResultsRecords.iterator().next().value();
        // Id should be equals to response id
        Assertions.assertEquals(processId, resultsDTO.getId());

        // Iterations and time between iterations should be equals to the initial object (POST request)
        Assertions.assertEquals(startFraudSimulationDTO.getIterations(), resultsDTO.getIterations());
        Assertions.assertEquals(startFraudSimulationDTO.getMsBetweenIterations(), resultsDTO.getMsBetweenIterations());
        Assertions.assertEquals(calculatePotentialFraudsMovements(startFraudSimulationDTO), resultsDTO.getPotentialFraudMovements());
        Assertions.assertNotNull(resultsDTO.getCardFraudConfig());
        Assertions.assertFalse(resultsDTO.getCardFraudConfig().isEmpty());
        Assertions.assertEquals(startFraudSimulationDTO.getCardsFraudConfig().size(), resultsDTO.getCardFraudConfig().size());

        resultsDTO.getCardFraudConfig().forEach((fraudConfig) -> {
            CardFraudConfigDTO cardFraudConfigDTO = startFraudSimulationDTO.getCardsFraudConfig().stream()
                    .filter((f) -> f.getCard().equalsIgnoreCase(fraudConfig.getCard()))
                    .collect(Collectors.toList())
                    .get(0);

            Assertions.assertNotNull(cardFraudConfigDTO);
            Assertions.assertEquals(cardFraudConfigDTO.getFraudTypes(), fraudConfig.getFraudTypes());
        });


        return resultsDTO;
    }

    private void checkMovementsCaseAll(SimulationDataResultsDTO resultsDTO) {
        ConsumerRecords<String, ATMMovementDTO> atmMovements = atmConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, OnlineMovementDTO> onlineMovements = onlineConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, MerchantMovementDTO> merchantMovements = merchantConsumer.poll(Duration.ofSeconds(5));

        Assertions.assertTrue(atmMovements.count() > 0);
        Assertions.assertTrue(merchantMovements.count() > 0);
        Assertions.assertTrue(onlineMovements.count() > 0);

        int totalMessagesInMovementsTopics = atmMovements.count() + onlineMovements.count() + merchantMovements.count();
        Assertions.assertEquals(totalMessagesInMovementsTopics, resultsDTO.getMovementsGenerated());
    }

    private void checkMovementsCaseMultipleATM(SimulationDataResultsDTO resultsDTO) {
        ConsumerRecords<String, ATMMovementDTO> atmMovements = atmConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, OnlineMovementDTO> onlineMovements = onlineConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, MerchantMovementDTO> merchantMovements = merchantConsumer.poll(Duration.ofSeconds(5));

        Assertions.assertEquals(atmMovements.count(), resultsDTO.getMovementsGenerated());
        Assertions.assertEquals(0, merchantMovements.count());
        Assertions.assertEquals(0, onlineMovements.count());
    }

    private void checkMovementsCaseMultipleMerchant(SimulationDataResultsDTO resultsDTO) {
        ConsumerRecords<String, ATMMovementDTO> atmMovements = atmConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, OnlineMovementDTO> onlineMovements = onlineConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, MerchantMovementDTO> merchantMovements = merchantConsumer.poll(Duration.ofSeconds(5));

        Assertions.assertEquals(0, atmMovements.count());
        Assertions.assertEquals(resultsDTO.getMovementsGenerated(), merchantMovements.count());
        Assertions.assertEquals(0, onlineMovements.count());
    }

    private void checkMovementsCaseMultipleOnline(SimulationDataResultsDTO resultsDTO) {
        ConsumerRecords<String, ATMMovementDTO> atmMovements = atmConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, OnlineMovementDTO> onlineMovements = onlineConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, MerchantMovementDTO> merchantMovements = merchantConsumer.poll(Duration.ofSeconds(5));

        Assertions.assertEquals(0, atmMovements.count());
        Assertions.assertEquals(0, merchantMovements.count());
        Assertions.assertEquals(resultsDTO.getMovementsGenerated(), onlineMovements.count());
    }

    private void checkMovementsCaseOnlinePotentialFraudSite(SimulationDataResultsDTO resultsDTO) {
        ConsumerRecords<String, ATMMovementDTO> atmMovements = atmConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, OnlineMovementDTO> onlineMovements = onlineConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, MerchantMovementDTO> merchantMovements = merchantConsumer.poll(Duration.ofSeconds(5));

        Assertions.assertEquals(0, atmMovements.count());
        Assertions.assertEquals(0, merchantMovements.count());
        Assertions.assertEquals(1 * resultsDTO.getIterations(), onlineMovements.count());
        Assertions.assertEquals(resultsDTO.getMovementsGenerated(), onlineMovements.count());
    }

    private void checkMovementsCaseATMAndMerchant(SimulationDataResultsDTO resultsDTO) {
        ConsumerRecords<String, ATMMovementDTO> atmMovements = atmConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, OnlineMovementDTO> onlineMovements = onlineConsumer.poll(Duration.ofSeconds(5));
        ConsumerRecords<String, MerchantMovementDTO> merchantMovements = merchantConsumer.poll(Duration.ofSeconds(5));

        Assertions.assertEquals(1 * resultsDTO.getIterations(), atmMovements.count());
        Assertions.assertEquals(1 * resultsDTO.getIterations(), merchantMovements.count());
        Assertions.assertEquals(0, onlineMovements.count());
        Assertions.assertEquals(resultsDTO.getMovementsGenerated(), atmMovements.count() + merchantMovements.count());
    }

    private Properties consumerProps(String groupId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaResource.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    public int calculatePotentialFraudsMovements(StartFraudSimulationDTO simulationData) {
        final List<CardFraudConfigDTO> cardFraudConfigs = simulationData.getCardsFraudConfig();
        int totalFraudCasesInOneIteration = cardFraudConfigs.stream().mapToInt(cardFraudConfig -> cardFraudConfig.getFraudTypes().size()).sum();

        return totalFraudCasesInOneIteration * simulationData.getIterations();
    }
}