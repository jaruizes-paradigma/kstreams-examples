package com.jaruizes.streaming.examples.kstreams.fraudchecker.extractors;

import com.jaruizes.streaming.examples.kstreams.fraudchecker.dto.CardMovementDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

public class CardMovementTimestampExtractor implements TimestampExtractor {

    @Override
    public long extract(ConsumerRecord<Object, Object> consumerRecord, long l) {
        CardMovementDTO cardMovement = (CardMovementDTO) consumerRecord.value();

        return cardMovement.getCreatedAt().getTime();
    }
}
