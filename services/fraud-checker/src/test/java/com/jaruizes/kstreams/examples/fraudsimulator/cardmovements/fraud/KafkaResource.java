package com.jaruizes.kstreams.examples.fraudsimulator.cardmovements.fraud;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class KafkaResource implements QuarkusTestResourceLifecycleManager {

    private final static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));

    @Override
    public Map<String, String> start() {
        kafka.start();

        Map<String, String> systemProperties = new HashMap<>();
        systemProperties.put("kafka.bootstrap.servers", kafka.getBootstrapServers());
        systemProperties.put("quarkus.kafka-streams.bootstrap-servers", kafka.getBootstrapServers());

        return systemProperties;
    }

    @Override
    public void stop() {
        kafka.close();
    }

    public static String getBootstrapServers() {
        return kafka.getBootstrapServers();
    }
}
