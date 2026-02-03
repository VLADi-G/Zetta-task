package com.zetta.task.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConditionEngineTest {
    
    private ConditionEngine conditionEngine;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        conditionEngine = new ConditionEngine();
    }
    
    @Test
    void testAllOperator_AllConditionsTrue_ReturnsTrue() throws Exception {
        // Given
        String conditionJson = """
            {
              "operator": "all",
              "conditions": [
                {
                  "field": "user.age",
                  "operator": "greaterThan",
                  "value": 18
                },
                {
                  "field": "user.status",
                  "operator": "equals",
                  "value": "active"
                }
              ]
            }
            """;
        
        String dataJson = """
            {
              "user": {
                "age": 25,
                "status": "active"
              }
            }
            """;
        
        JsonNode condition = objectMapper.readTree(conditionJson);
        JsonNode data = objectMapper.readTree(dataJson);
        
        // When
        boolean result = conditionEngine.evaluate(condition, data);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testAllOperator_OneConditionFalse_ReturnsFalse() throws Exception {
        // Given
        String conditionJson = """
            {
              "operator": "all",
              "conditions": [
                {
                  "field": "user.age",
                  "operator": "greaterThan",
                  "value": 18
                },
                {
                  "field": "user.status",
                  "operator": "equals",
                  "value": "active"
                }
              ]
            }
            """;
        
        String dataJson = """
            {
              "user": {
                "age": 25,
                "status": "blocked"
              }
            }
            """;
        
        JsonNode condition = objectMapper.readTree(conditionJson);
        JsonNode data = objectMapper.readTree(dataJson);
        
        // When
        boolean result = conditionEngine.evaluate(condition, data);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testAnyOperator_AtLeastOneTrue_ReturnsTrue() throws Exception {
        // Given
        String conditionJson = """
            {
              "operator": "any",
              "conditions": [
                {
                  "field": "user.country",
                  "operator": "equals",
                  "value": "US"
                },
                {
                  "field": "user.country",
                  "operator": "equals",
                  "value": "CA"
                }
              ]
            }
            """;
        
        String dataJson = """
            {
              "user": {
                "country": "CA"
              }
            }
            """;
        
        JsonNode condition = objectMapper.readTree(conditionJson);
        JsonNode data = objectMapper.readTree(dataJson);
        
        // When
        boolean result = conditionEngine.evaluate(condition, data);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testNoneOperator_NoConditionsTrue_ReturnsTrue() throws Exception {
        // Given
        String conditionJson = """
            {
              "operator": "none",
              "conditions": [
                {
                  "field": "user.status",
                  "operator": "equals",
                  "value": "blocked"
                },
                {
                  "field": "user.status",
                  "operator": "equals",
                  "value": "suspended"
                }
              ]
            }
            """;
        
        String dataJson = """
            {
              "user": {
                "status": "active"
              }
            }
            """;
        
        JsonNode condition = objectMapper.readTree(conditionJson);
        JsonNode data = objectMapper.readTree(dataJson);
        
        // When
        boolean result = conditionEngine.evaluate(condition, data);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testNoneOperator_OneConditionTrue_ReturnsFalse() throws Exception {
        // Given
        String conditionJson = """
            {
              "operator": "none",
              "conditions": [
                {
                  "field": "user.status",
                  "operator": "equals",
                  "value": "blocked"
                }
              ]
            }
            """;
        
        String dataJson = """
            {
              "user": {
                "status": "blocked"
              }
            }
            """;
        
        JsonNode condition = objectMapper.readTree(conditionJson);
        JsonNode data = objectMapper.readTree(dataJson);
        
        // When
        boolean result = conditionEngine.evaluate(condition, data);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testNestedConditions_ComplexLogic_EvaluatesCorrectly() throws Exception {
        // Given
        String conditionJson = """
            {
              "operator": "all",
              "conditions": [
                {
                  "field": "user.age",
                  "operator": "greaterThanOrEqual",
                  "value": 18
                },
                {
                  "operator": "any",
                  "conditions": [
                    {
                      "field": "user.country",
                      "operator": "equals",
                      "value": "US"
                    },
                    {
                      "field": "user.country",
                      "operator": "equals",
                      "value": "CA"
                    }
                  ]
                }
              ]
            }
            """;
        
        String dataJson = """
            {
              "user": {
                "age": 25,
                "country": "US"
              }
            }
            """;
        
        JsonNode condition = objectMapper.readTree(conditionJson);
        JsonNode data = objectMapper.readTree(dataJson);
        
        // When
        boolean result = conditionEngine.evaluate(condition, data);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testContainsOperator_SubstringExists_ReturnsTrue() throws Exception {
        // Given
        String conditionJson = """
            {
              "field": "user.email",
              "operator": "contains",
              "value": "@example.com"
            }
            """;
        
        String dataJson = """
            {
              "user": {
                "email": "john@example.com"
              }
            }
            """;
        
        JsonNode condition = objectMapper.readTree(conditionJson);
        JsonNode data = objectMapper.readTree(dataJson);
        
        // When
        boolean result = conditionEngine.evaluate(condition, data);
        
        // Then
        assertTrue(result);
    }
}
