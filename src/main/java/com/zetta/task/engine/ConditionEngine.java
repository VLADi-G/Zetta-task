package com.zetta.task.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zetta.task.exception.ConditionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ConditionEngine {
    
    private final ObjectMapper objectMapper;
    
    public ConditionEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Evaluates a condition against the provided data.
     * 
     * @param condition The condition rule in JSON format
     * @param data The data to evaluate against
     * @return true if the condition is met, false otherwise
     */
    public boolean evaluate(JsonNode condition, JsonNode data) {
        try {
            String operator = condition.get("operator").asText();
            
            return switch (operator) {
                case "all" -> evaluateAll(condition.get("conditions"), data);
                case "any" -> evaluateAny(condition.get("conditions"), data);
                case "none" -> evaluateNone(condition.get("conditions"), data);
                default -> evaluateSingleCondition(condition, data);
            };
        } catch (Exception e) {
            log.error("Error evaluating condition: {}", e.getMessage());
            throw new ConditionException("Failed to evaluate condition", e);
        }
    }
    
    /**
     * Evaluates 'all' operator - all conditions must be true (AND logic)
     */
    private boolean evaluateAll(JsonNode conditions, JsonNode data) {
        if (conditions == null || !conditions.isArray()) {
            throw new ConditionException("'all' operator requires an array of conditions");
        }
        
        for (JsonNode condition : conditions) {
            if (!evaluate(condition, data)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Evaluates 'any' operator - at least one condition must be true (OR logic)
     */
    private boolean evaluateAny(JsonNode conditions, JsonNode data) {
        if (conditions == null || !conditions.isArray()) {
            throw new ConditionException("'any' operator requires an array of conditions");
        }
        
        for (JsonNode condition : conditions) {
            if (evaluate(condition, data)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Evaluates 'none' operator - no conditions must be true (NOT logic)
     */
    private boolean evaluateNone(JsonNode conditions, JsonNode data) {
        if (conditions == null || !conditions.isArray()) {
            throw new ConditionException("'none' operator requires an array of conditions");
        }
        
        for (JsonNode condition : conditions) {
            if (evaluate(condition, data)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Evaluates a single condition with field, operator, and value
     */
    private boolean evaluateSingleCondition(JsonNode condition, JsonNode data) {
        String field = condition.get("field").asText();
        String operator = condition.get("operator").asText();
        JsonNode expectedValue = condition.get("value");
        
        JsonNode actualValue = getFieldValue(data, field);
        
        if (actualValue == null || actualValue.isNull()) {
            return false;
        }
        
        return switch (operator) {
            case "equals" -> actualValue.equals(expectedValue);
            case "notEquals" -> !actualValue.equals(expectedValue);
            case "greaterThan" -> actualValue.asDouble() > expectedValue.asDouble();
            case "lessThan" -> actualValue.asDouble() < expectedValue.asDouble();
            case "greaterThanOrEqual" -> actualValue.asDouble() >= expectedValue.asDouble();
            case "lessThanOrEqual" -> actualValue.asDouble() <= expectedValue.asDouble();
            case "contains" -> actualValue.asText().contains(expectedValue.asText());
            default -> throw new ConditionException("Unknown operator: " + operator);
        };
    }
    
    /**
     * Gets a field value from data using dot notation (e.g., "user.name")
     */
    private JsonNode getFieldValue(JsonNode data, String field) {
        String[] parts = field.split("\\.");
        JsonNode current = data;
        
        for (String part : parts) {
            if (current == null || current.isNull()) {
                return null;
            }
            current = current.get(part);
        }
        
        return current;
    }
}
