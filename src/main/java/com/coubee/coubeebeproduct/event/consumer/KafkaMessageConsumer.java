package com.coubee.coubeebeproduct.event.consumer;

import com.coubee.coubeebeproduct.event.consumer.message.StockIncreaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessageConsumer {

    @KafkaListener(
            topics = StockIncreaseEvent.Topic,
            properties = {
                    JsonDeserializer.VALUE_DEFAULT_TYPE + ":com.coubee.coubeebeproduct.event.consumer.message.StockIncreaseEvent"
            }
    )
    void handleProductEvent(StockIncreaseEvent event, Acknowledgment ack) {
        log.info("product event: {}", event);
        ack.acknowledge();
    }

}
