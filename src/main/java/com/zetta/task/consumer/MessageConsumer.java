package com.zetta.task.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.zetta.task.service.MessageProcessor;

@Component
public class MessageConsumer {

	private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);
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
		}
	}
}
