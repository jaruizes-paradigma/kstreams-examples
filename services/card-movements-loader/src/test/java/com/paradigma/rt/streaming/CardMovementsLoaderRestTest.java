package com.paradigma.rt.streaming;

import com.paradigma.rt.streaming.cardmovements.loader.api.rest.dto.ATMMovementDTO;
import com.paradigma.rt.streaming.cardmovements.loader.api.rest.dto.CreateMovementResponseDTO;
import com.paradigma.rt.streaming.cardmovements.loader.api.rest.dto.MerchantMovementDTO;
import com.paradigma.rt.streaming.cardmovements.loader.api.rest.dto.OnlineMovementDTO;
import com.paradigma.rt.streaming.cardmovements.loader.model.ATMMovement;
import com.paradigma.rt.streaming.cardmovements.loader.model.CardMovement;
import com.paradigma.rt.streaming.cardmovements.loader.model.MerchantMovement;
import com.paradigma.rt.streaming.cardmovements.loader.model.OnlineMovement;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(KafkaResource.class)
public class CardMovementsLoaderRestTest {

    @ConfigProperty(name = "mp.messaging.outgoing.atm-movements-out.topic")
    String atmMovementsTopic;

    @ConfigProperty(name = "mp.messaging.outgoing.online-movements-out.topic")
    String onlineMovementsTopic;

    @ConfigProperty(name = "mp.messaging.outgoing.merchant-movements-out.topic")
    String merchantMovementsTopic;

    KafkaProducer<String, ATMMovement> atmProducer;
    KafkaConsumer<String, ATMMovement> atmConsumer;
    KafkaConsumer<String, OnlineMovement> onlineConsumer;
    KafkaConsumer<String, MerchantMovement> merchantConsumer;

    @BeforeEach
    public void setUp(){
        atmConsumer = new KafkaConsumer(consumerProps("atm"), new StringDeserializer(), new ObjectMapperDeserializer<>(ATMMovement.class));
        onlineConsumer = new KafkaConsumer(consumerProps("online"), new StringDeserializer(), new ObjectMapperDeserializer<>(OnlineMovement.class));
        merchantConsumer = new KafkaConsumer(consumerProps("merchant"), new StringDeserializer(), new ObjectMapperDeserializer<>(MerchantMovement.class));
    }

    @AfterEach
    public void tearDown(){
        atmConsumer.close();
        onlineConsumer.close();
        merchantConsumer.close();
    }

    @Test
    @Timeout(value = 30)
    public void when_WeInvokeRESTAPIToCreateAnATMMovement_Then_MovementDataIsPublishedToKafka() {
        atmConsumer.subscribe(Collections.singletonList(atmMovementsTopic));

        final ATMMovementDTO atmMovementDTO = new ATMMovementDTO();
        atmMovementDTO.setAtm("test");
        atmMovementDTO.setCard("1234");
        atmMovementDTO.setAmount(10.25f);
        CreateMovementResponseDTO movementResponseDTO =
                given()
                .body(atmMovementDTO)
                .contentType("application/json")
                .when()
                .post("/cards-movements/atm")
                .then()
                .statusCode(201)
                .extract()
                .as(CreateMovementResponseDTO.class);

        Assertions.assertNotNull(movementResponseDTO);
        Assertions.assertNotNull(movementResponseDTO.getId());
        Assertions.assertTrue(movementResponseDTO.getId().startsWith("atm"));

        ConsumerRecords<String, ATMMovement> atmMovements = atmConsumer.poll(Duration.ofSeconds(10));
        Assertions.assertNotNull(atmMovements);
        Assertions.assertFalse(atmMovements.isEmpty());

        ATMMovement movementCreated = atmMovements.records(atmMovementsTopic).iterator().next().value();
        Assertions.assertNotNull(movementCreated);
        Assertions.assertEquals(movementResponseDTO.getId(), movementCreated.getId());
        Assertions.assertEquals("1234", movementCreated.getCard());
        Assertions.assertEquals("test", movementCreated.getAtm());
        Assertions.assertEquals(10.25f, movementCreated.getAmount());
    }

    @Test
    @Timeout(value = 30)
    public void when_WeInvokeRESTAPIToCreateAnOnlineMovement_Then_MovementDataIsPublishedToKafka() {
        onlineConsumer.subscribe(Collections.singletonList(onlineMovementsTopic));

        final OnlineMovementDTO onlineMovementDTO = new OnlineMovementDTO();
        onlineMovementDTO.setSite("test");
        onlineMovementDTO.setCard("1234");
        onlineMovementDTO.setAmount(10.25f);
        CreateMovementResponseDTO movementResponseDTO =
                given()
                        .body(onlineMovementDTO)
                        .contentType("application/json")
                        .when()
                        .post("/cards-movements/online")
                        .then()
                        .statusCode(201)
                        .extract()
                        .as(CreateMovementResponseDTO.class);

        Assertions.assertNotNull(movementResponseDTO);
        Assertions.assertNotNull(movementResponseDTO.getId());
        Assertions.assertTrue(movementResponseDTO.getId().startsWith("online"));

        ConsumerRecords<String, OnlineMovement> onlineMovements = onlineConsumer.poll(Duration.ofSeconds(10));
        Assertions.assertNotNull(onlineMovements);
        Assertions.assertFalse(onlineMovements.isEmpty());

        OnlineMovement movementCreated = onlineMovements.records(onlineMovementsTopic).iterator().next().value();

        Assertions.assertNotNull(movementCreated);
        Assertions.assertEquals(movementResponseDTO.getId(), movementCreated.getId());
        Assertions.assertEquals("1234", movementCreated.getCard());
        Assertions.assertEquals("test", movementCreated.getSite());
        Assertions.assertEquals(10.25f, movementCreated.getAmount());
    }

    @Test
    @Timeout(value = 30)
    public void when_WeInvokeRESTAPIToCreateAMerchantMovement_Then_MovementDataIsPublishedToKafka() {
        merchantConsumer.subscribe(Collections.singletonList(merchantMovementsTopic));

        final MerchantMovementDTO merchantMovementDTO = new MerchantMovementDTO();
        merchantMovementDTO.setMerchant("test");
        merchantMovementDTO.setCard("1234");
        merchantMovementDTO.setAmount(10.25f);
        CreateMovementResponseDTO movementResponseDTO =
                given()
                        .body(merchantMovementDTO)
                        .contentType("application/json")
                        .when()
                        .post("/cards-movements/merchant")
                        .then()
                        .statusCode(201)
                        .extract()
                        .as(CreateMovementResponseDTO.class);

        Assertions.assertNotNull(movementResponseDTO);
        Assertions.assertNotNull(movementResponseDTO.getId());
        Assertions.assertTrue(movementResponseDTO.getId().startsWith("merchant"));

        ConsumerRecords<String, MerchantMovement> merchantMovements = merchantConsumer.poll(Duration.ofSeconds(10));
        Assertions.assertNotNull(merchantMovements);
        Assertions.assertFalse(merchantMovements.isEmpty());

        MerchantMovement movementCreated = merchantMovements.records(merchantMovementsTopic).iterator().next().value();

        Assertions.assertNotNull(movementCreated);
        Assertions.assertEquals(movementResponseDTO.getId(), movementCreated.getId());
        Assertions.assertEquals("1234", movementCreated.getCard());
        Assertions.assertEquals("test", movementCreated.getMerchant());
        Assertions.assertEquals(10.25f, movementCreated.getAmount());
    }

    private Properties consumerProps(String groupId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaResource.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}