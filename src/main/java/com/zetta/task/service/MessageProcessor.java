package com.zetta.task.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zetta.task.engine.ConditionEngine;
import com.zetta.task.engine.TransformationEngine;
import com.zetta.task.model.MessageState;
import com.zetta.task.model.StateDelta;
import com.zetta.task.producer.MessageProducer;
import com.zetta.task.repository.MessageStateRepository;
import com.zetta.task.repository.StateDeltaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class MessageProcessor {
    
    private final ConditionEngine conditionEngine;
    private final TransformationEngine transformationEngine;
    private final MessageStateRepository messageStateRepository;
    private final StateDeltaRepository stateDeltaRepository;
    private final MessageProducer messageProducer;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    
    @Value("${app.rules.condition-file}")
    private String conditionRulesPath;
    
    @Value("${app.rules.transformation-file}")
    private String transformationRulesPath;
    
    private JsonNode conditionRules;
    private JsonNode transformationRules;
    
    public MessageProcessor(
            ConditionEngine conditionEngine,
            TransformationEngine transformationEngine,
            MessageStateRepository messageStateRepository,
            StateDeltaRepository stateDeltaRepository,
            MessageProducer messageProducer,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader) {
        this.conditionEngine = conditionEngine;
        this.transformationEngine = transformationEngine;
        this.messageStateRepository = messageStateRepository;
        this.stateDeltaRepository = stateDeltaRepository;
        this.messageProducer = messageProducer;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }
    
    @PostConstruct
    public void loadRules() throws IOException {
        log.info("Loading condition rules from: {}", conditionRulesPath);
        Resource conditionResource = resourceLoader.getResource(conditionRulesPath);
        conditionRules = objectMapper.readTree(conditionResource.getInputStream());
        
        log.info("Loading transformation rules from: {}", transformationRulesPath);
        Resource transformationResource = resourceLoader.getResource(transformationRulesPath);
        transformationRules = objectMapper.readTree(transformationResource.getInputStream());
        
        log.info("Rules loaded successfully");
    }
    
    /**
     * Processes an incoming message through the complete pipeline:
     * 1. Evaluate conditions
     * 2. Apply transformations
     * 3. Persist state and delta
     * 4. Publish to output topic
     */
    @Transactional
    public void processMessage(String messagePayload) {
        try {
            log.info("Processing message: {}", messagePayload);
            
            // Parse incoming message
            JsonNode data = objectMapper.readTree(messagePayload);
            
            // Evaluate conditions
            boolean conditionsMet = evaluateConditions(data);
            
            if (!conditionsMet) {
                log.info("Conditions not met, skipping message");
                return;
            }
            
            // Store original state
            String originalState = objectMapper.writeValueAsString(data);
            
            // Apply transformations
            JsonNode transformedData = applyTransformations(data);
            
            // Generate message ID
            String messageId = data.has("id") ? data.get("id").asText() : UUID.randomUUID().toString();
            
            // Persist state and delta
            MessageState state = persistState(messageId, transformedData);
            persistDelta(state.getId(), originalState, objectMapper.writeValueAsString(transformedData));
            
            // Publish to output topic
            publishResult(transformedData);
            
            log.info("Message processed successfully with ID: {}", messageId);
            
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process message", e);
        }
    }
    
    /**
     * Evaluates all condition rules against the data
     */
    private boolean evaluateConditions(JsonNode data) {
        if (conditionRules == null || conditionRules.isNull()) {
            log.debug("No condition rules defined, allowing message");
            return true;
        }
        
        boolean result = conditionEngine.evaluate(conditionRules, data);
        log.debug("Condition evaluation result: {}", result);
        return result;
    }
    
    /**
     * Applies all transformation rules to the data
     */
    private JsonNode applyTransformations(JsonNode data) {
        if (transformationRules == null || transformationRules.isNull()) {
            log.debug("No transformation rules defined, returning original data");
            return data;
        }
        
        JsonNode transformed = transformationEngine.apply(transformationRules, data);
        log.debug("Transformation applied successfully");
        return transformed;
    }
    
    /**
     * Persists the current state of the message
     */
    private MessageState persistState(String messageId, JsonNode data) throws IOException {
        String stateJson = objectMapper.writeValueAsString(data);
        
        MessageState state = messageStateRepository.findByMessageId(messageId)
                .orElse(MessageState.builder()
                        .messageId(messageId)
                        .build());
        
        state.setCurrentState(stateJson);
        state = messageStateRepository.save(state);
        
        log.debug("State persisted for message ID: {}", messageId);
        return state;
    }
    
    /**
     * Persists the delta (difference) between states
     */
    private void persistDelta(Long messageStateId, String beforeState, String afterState) throws IOException {
        // Calculate changes
        String changes = calculateChanges(beforeState, afterState);
        
        StateDelta delta = StateDelta.builder()
                .messageStateId(messageStateId)
                .beforeState(beforeState)
                .afterState(afterState)
                .changes(changes)
                .build();
        
        stateDeltaRepository.save(delta);
        log.debug("Delta persisted for message state ID: {}", messageStateId);
    }
    
    /**
     * Calculates the changes between two states
     */
    private String calculateChanges(String beforeState, String afterState) throws IOException {
        JsonNode before = objectMapper.readTree(beforeState);
        JsonNode after = objectMapper.readTree(afterState);
        
        // Simple diff: just note that transformation was applied
        // In a more sophisticated implementation, this would calculate field-by-field differences
        return String.format("Transformation applied at %s", java.time.LocalDateTime.now());
    }
    
    /**
     * Publishes the result to the output topic
     */
    private void publishResult(JsonNode data) throws IOException {
        String message = objectMapper.writeValueAsString(data);
        messageProducer.sendMessage(message);
        log.debug("Result published to output topic");
    }
}
