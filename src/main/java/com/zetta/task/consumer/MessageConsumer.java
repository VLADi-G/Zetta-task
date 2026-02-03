package com.zetta.task.consumer;

import com.zetta.task.service.MessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageConsumer {
    
    private final MessageProcessor messageProcessor;
    
    public MessageConsumer(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }
    
    @KafkaListener(topics = "${app.kafka.input-topic}", groupId = "${app.kafka.consumer-group}")
    public void consume(String message) {
        log.info("Received message from Kafka: {}", message);
        
        try {
            messageProcessor.processMessage(message);
        } catch (Exception e) {
            log.error("Failed to process message: {}", e.getMessage(), e);
            // In production, you might want to send to DLQ (Dead Letter Queue)
        }
    }
}
