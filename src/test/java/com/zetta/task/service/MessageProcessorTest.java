package com.zetta.task.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zetta.task.engine.ConditionEngine;
import com.zetta.task.engine.TransformationEngine;
import com.zetta.task.model.MessageState;
import com.zetta.task.producer.MessageProducer;
import com.zetta.task.repository.MessageStateRepository;
import com.zetta.task.repository.StateDeltaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {
    
    @Mock
    private ConditionEngine conditionEngine;
    
    @Mock
    private TransformationEngine transformationEngine;
    
    @Mock
    private MessageStateRepository messageStateRepository;
    
    @Mock
    private StateDeltaRepository stateDeltaRepository;
    
    @Mock
    private MessageProducer messageProducer;
    
    @Mock
    private ResourceLoader resourceLoader;
    
    private MessageProcessor messageProcessor;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        messageProcessor = new MessageProcessor(
                conditionEngine,
                transformationEngine,
                messageStateRepository,
                stateDeltaRepository,
                messageProducer,
                objectMapper,
                resourceLoader
        );
    }
    
    @Test
    void testProcessMessage_ConditionsMet_ProcessesSuccessfully() throws Exception {
        // Given
        String messagePayload = """
            {
              "id": "test-001",
              "data": {
                "user": {
                  "name": "John",
                  "age": 25
                }
              }
            }
            """;
        
        JsonNode inputData = objectMapper.readTree(messagePayload);
        JsonNode transformedData = objectMapper.readTree("""
            {
              "id": "test-001",
              "data": {
                "user": {
                  "name": "John",
                  "age": 26
                }
              }
            }
            """);
        
        MessageState mockState = MessageState.builder()
                .id(1L)
                .messageId("test-001")
                .currentState(objectMapper.writeValueAsString(transformedData))
                .build();
        
        // Mock rule loading (simplified for test)
        messageProcessor.conditionRules = objectMapper.createObjectNode();
        messageProcessor.transformationRules = objectMapper.createObjectNode();
        
        when(conditionEngine.evaluate(any(JsonNode.class), any(JsonNode.class))).thenReturn(true);
        when(transformationEngine.apply(any(JsonNode.class), any(JsonNode.class))).thenReturn(transformedData);
        when(messageStateRepository.findByMessageId("test-001")).thenReturn(Optional.empty());
        when(messageStateRepository.save(any(MessageState.class))).thenReturn(mockState);
        
        // When
        messageProcessor.processMessage(messagePayload);
        
        // Then
        verify(conditionEngine, times(1)).evaluate(any(JsonNode.class), any(JsonNode.class));
        verify(transformationEngine, times(1)).apply(any(JsonNode.class), any(JsonNode.class));
        verify(messageStateRepository, times(1)).save(any(MessageState.class));
        verify(stateDeltaRepository, times(1)).save(any());
        verify(messageProducer, times(1)).sendMessage(anyString());
    }
    
    @Test
    void testProcessMessage_ConditionsNotMet_SkipsProcessing() throws Exception {
        // Given
        String messagePayload = """
            {
              "id": "test-002",
              "data": {
                "user": {
                  "name": "Jane",
                  "age": 15
                }
              }
            }
            """;
        
        // Mock rule loading
        messageProcessor.conditionRules = objectMapper.createObjectNode();
        messageProcessor.transformationRules = objectMapper.createObjectNode();
        
        when(conditionEngine.evaluate(any(JsonNode.class), any(JsonNode.class))).thenReturn(false);
        
        // When
        messageProcessor.processMessage(messagePayload);
        
        // Then
        verify(conditionEngine, times(1)).evaluate(any(JsonNode.class), any(JsonNode.class));
        verify(transformationEngine, never()).apply(any(), any());
        verify(messageStateRepository, never()).save(any());
        verify(messageProducer, never()).sendMessage(anyString());
    }
    
    @Test
    void testProcessMessage_UpdatesExistingState() throws Exception {
        // Given
        String messagePayload = """
            {
              "id": "test-003",
              "data": {
                "user": {
                  "name": "Bob",
                  "age": 30
                }
              }
            }
            """;
        
        JsonNode transformedData = objectMapper.readTree(messagePayload);
        
        MessageState existingState = MessageState.builder()
                .id(1L)
                .messageId("test-003")
                .currentState("{}")
                .build();
        
        // Mock rule loading
        messageProcessor.conditionRules = objectMapper.createObjectNode();
        messageProcessor.transformationRules = objectMapper.createObjectNode();
        
        when(conditionEngine.evaluate(any(JsonNode.class), any(JsonNode.class))).thenReturn(true);
        when(transformationEngine.apply(any(JsonNode.class), any(JsonNode.class))).thenReturn(transformedData);
        when(messageStateRepository.findByMessageId("test-003")).thenReturn(Optional.of(existingState));
        when(messageStateRepository.save(any(MessageState.class))).thenReturn(existingState);
        
        // When
        messageProcessor.processMessage(messagePayload);
        
        // Then
        verify(messageStateRepository, times(1)).findByMessageId("test-003");
        verify(messageStateRepository, times(1)).save(argThat(state -> 
            state.getMessageId().equals("test-003") && state.getId().equals(1L)
        ));
    }
}
