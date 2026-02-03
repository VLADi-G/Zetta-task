package com.zetta.task.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageProducer {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("${app.kafka.output-topic}")
    private String outputTopic;
    
    public MessageProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendMessage(String message) {
        log.info("Sending message to topic {}: {}", outputTopic, message);
        kafkaTemplate.send(outputTopic, message);
    }
}
