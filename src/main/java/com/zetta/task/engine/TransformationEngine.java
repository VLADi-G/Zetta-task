package com.zetta.task.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zetta.task.exception.TransformationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TransformationEngine {
    
    private static final Logger log = LoggerFactory.getLogger(TransformationEngine.class);
    private final ObjectMapper objectMapper;
    
    // Patterns for parsing transformation expressions
    private static final Pattern SET_PATTERN = Pattern.compile("set\\s+([\\w.]+)\\s*=\\s*(.+)");
    private static final Pattern REMOVE_PATTERN = Pattern.compile("remove\\s+([\\w.]+)");
    private static final Pattern FIELD_PATTERN = Pattern.compile("([\\w.]+)");
    
    public TransformationEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Applies a transformation rule to the data
     * 
     * @param transformation The transformation rule
     * @param data The data to transform
     * @return The transformed data
     */
    public JsonNode apply(JsonNode transformation, JsonNode data) {
        try {
            ObjectNode mutableData = data.deepCopy();
            
            if (transformation.has("rules") && transformation.get("rules").isArray()) {
                for (JsonNode rule : transformation.get("rules")) {
                    String expression = rule.asText();
                    applyExpression(expression, mutableData);
                }
            }
            
            return mutableData;
        } catch (Exception e) {
            log.error("Error applying transformation: {}", e.getMessage());
            throw new TransformationException("Failed to apply transformation", e);
        }
    }
    
    /**
     * Applies a single transformation expression
     */
    private void applyExpression(String expression, ObjectNode data) {
        expression = expression.trim();
        
        // Handle SET operations
        Matcher setMatcher = SET_PATTERN.matcher(expression);
        if (setMatcher.matches()) {
            String field = setMatcher.group(1);
            String valueExpression = setMatcher.group(2).trim();
            Object value = evaluateExpression(valueExpression, data);
            setFieldValue(data, field, value);
            return;
        }
        
        // Handle REMOVE operations
        Matcher removeMatcher = REMOVE_PATTERN.matcher(expression);
        if (removeMatcher.matches()) {
            String field = removeMatcher.group(1);
            removeField(data, field);
            return;
        }
        
        throw new TransformationException("Invalid transformation expression: " + expression);
    }
    
    /**
     * Evaluates an expression (arithmetic, string concatenation, field references)
     */
    private Object evaluateExpression(String expression, JsonNode data) {
        expression = expression.trim();
        
        // Handle string literals
        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            return expression.substring(1, expression.length() - 1);
        }
        
        // Handle numeric literals
        try {
            if (expression.contains(".")) {
                return Double.parseDouble(expression);
            } else {
                return Integer.parseInt(expression);
            }
        } catch (NumberFormatException e) {
            // Not a number, continue
        }
        
        // Handle string concatenation
        if (expression.contains("+")) {
            return evaluateConcatenation(expression, data);
        }
        
        // Handle arithmetic operations
        if (expression.matches(".*[\\-*/].*")) {
            return evaluateArithmetic(expression, data);
        }
        
        // Handle field reference
        JsonNode value = getFieldValue(data, expression);
        if (value == null) {
            return null;
        }
        
        if (value.isTextual()) {
            return value.asText();
        } else if (value.isInt()) {
            return value.asInt();
        } else if (value.isDouble()) {
            return value.asDouble();
        } else if (value.isBoolean()) {
            return value.asBoolean();
        }
        
        return value.toString();
    }
    
    /**
     * Evaluates string concatenation
     */
    private String evaluateConcatenation(String expression, JsonNode data) {
        String[] parts = expression.split("\\+");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            part = part.trim();
            Object value = evaluateExpression(part, data);
            if (value != null) {
                result.append(value);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Evaluates arithmetic operations
     */
    private double evaluateArithmetic(String expression, JsonNode data) {
        // Simple left-to-right evaluation
        String[] tokens = expression.split("\\s+");
        
        if (tokens.length < 3) {
            throw new TransformationException("Invalid arithmetic expression: " + expression);
        }
        
        double result = getNumericValue(tokens[0], data);
        
        for (int i = 1; i < tokens.length; i += 2) {
            if (i + 1 >= tokens.length) {
                break;
            }
            
            String operator = tokens[i];
            double operand = getNumericValue(tokens[i + 1], data);
            
            result = switch (operator) {
                case "+" -> result + operand;
                case "-" -> result - operand;
                case "*" -> result * operand;
                case "/" -> result / operand;
                default -> throw new TransformationException("Unknown operator: " + operator);
            };
        }
        
        return result;
    }
    
    /**
     * Gets a numeric value from a token (either literal or field reference)
     */
    private double getNumericValue(String token, JsonNode data) {
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            JsonNode value = getFieldValue(data, token);
            if (value == null || value.isNull()) {
                throw new TransformationException("Field not found or null: " + token);
            }
            return value.asDouble();
        }
    }
    
    /**
     * Gets a field value from data using dot notation
     */
    private JsonNode getFieldValue(JsonNode data, String field) {
        String[] parts = field.split("\\.");
        JsonNode current = data;
        
        for (String part : parts) {
            if (current == null || current.isNull()) {
                return null;
            }
            
            // Handle array access
            if (part.contains("[") && part.contains("]")) {
                String arrayField = part.substring(0, part.indexOf("["));
                int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                current = current.get(arrayField);
                if (current != null && current.isArray()) {
                    current = current.get(index);
                }
            } else {
                current = current.get(part);
            }
        }
        
        return current;
    }
    
    /**
     * Sets a field value in data using dot notation
     */
    private void setFieldValue(ObjectNode data, String field, Object value) {
        String[] parts = field.split("\\.");
        ObjectNode current = data;
        
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            
            if (!current.has(part)) {
                current.putObject(part);
            }
            
            JsonNode next = current.get(part);
            if (next instanceof ObjectNode) {
                current = (ObjectNode) next;
            } else {
                ObjectNode newNode = objectMapper.createObjectNode();
                current.set(part, newNode);
                current = newNode;
            }
        }
        
        String lastPart = parts[parts.length - 1];
        
        if (value instanceof String) {
            current.put(lastPart, (String) value);
        } else if (value instanceof Integer) {
            current.put(lastPart, (Integer) value);
        } else if (value instanceof Double) {
            current.put(lastPart, (Double) value);
        } else if (value instanceof Boolean) {
            current.put(lastPart, (Boolean) value);
        } else if (value == null) {
            current.putNull(lastPart);
        } else {
            current.put(lastPart, value.toString());
        }
    }
    
    /**
     * Removes a field from data using dot notation
     */
    private void removeField(ObjectNode data, String field) {
        String[] parts = field.split("\\.");
        ObjectNode current = data;
        
        for (int i = 0; i < parts.length - 1; i++) {
            JsonNode next = current.get(parts[i]);
            if (!(next instanceof ObjectNode)) {
                return; // Field doesn't exist
            }
            current = (ObjectNode) next;
        }
        
        current.remove(parts[parts.length - 1]);
    }
}
