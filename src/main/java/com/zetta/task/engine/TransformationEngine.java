package com.zetta.task.engine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zetta.task.exception.TransformationException;

@Component
public class TransformationEngine {

	private static final Logger log = LoggerFactory.getLogger(TransformationEngine.class);
	private final ObjectMapper objectMapper;

	public TransformationEngine(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	private static final Pattern SET_PATTERN = Pattern.compile("set\\s+([\\w.]+)\\s*=\\s*(.+)");
	private static final Pattern REMOVE_PATTERN = Pattern.compile("remove\\s+([\\w.]+)");

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

	private void applyExpression(String expression, ObjectNode data) {
		expression = expression.trim();

		Matcher setMatcher = SET_PATTERN.matcher(expression);
		if (setMatcher.matches()) {
			String field = setMatcher.group(1);
			String valueExpression = setMatcher.group(2).trim();
			Object value = evaluateExpression(valueExpression, data);
			setFieldValue(data, field, value);
			return;
		}

		Matcher removeMatcher = REMOVE_PATTERN.matcher(expression);
		if (removeMatcher.matches()) {
			String field = removeMatcher.group(1);
			removeField(data, field);
			return;
		}

		throw new TransformationException("Invalid transformation expression: " + expression);
	}

	private Object evaluateExpression(String expression, JsonNode data) {
		expression = expression.trim();

		if (expression.startsWith("\"") && expression.endsWith("\"")) {
			return expression.substring(1, expression.length() - 1);
		}

		try {
			if (expression.contains(".")) {
				return Double.parseDouble(expression);
			} else {
				return Integer.parseInt(expression);
			}
		} catch (NumberFormatException e) {
			log.debug("Not a numeric literal: {}", expression);
		}

		if (expression.matches(".*[\\-*/].*")) {
			return evaluateArithmetic(expression, data);
		}

		if (expression.contains("+")) {
			String[] parts = expression.split("\\+");
			boolean allNumeric = true;
			for (String part : parts) {
				part = part.trim();
				if (!isNumericExpression(part, data)) {
					allNumeric = false;
					break;
				}
			}

			if (allNumeric) {
				return evaluateArithmetic(expression, data);
			} else {
				return evaluateConcatenation(expression, data);
			}
		}

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

	private boolean isNumericExpression(String expression, JsonNode data) {
		expression = expression.trim();

		if (expression.startsWith("\"") && expression.endsWith("\"")) {
			return false;
		}

		try {
			Double.parseDouble(expression);
			return true;
		} catch (NumberFormatException e) {
			log.debug("Not a numeric literal: {}", expression);
		}

		JsonNode value = getFieldValue(data, expression);
		return value != null && (value.isInt() || value.isDouble() || value.isNumber());
	}

	private String evaluateConcatenation(String expression, JsonNode data) {
		String[] parts = expression.split("\\+");
		StringBuilder result = new StringBuilder();

		for (String part : parts) {
			part = part.trim();

			if (part.startsWith("\"") && part.endsWith("\"")) {
				result.append(part.substring(1, part.length() - 1));
				continue;
			}

			JsonNode value = getFieldValue(data, part);
			if (value != null) {
				if (value.isTextual()) {
					result.append(value.asText());
				} else {
					result.append(value.toString());
				}
			} else {
				result.append(part);
			}
		}

		return result.toString();
	}

	private double evaluateArithmetic(String expression, JsonNode data) {
		expression = expression.replaceAll("\\s*([+\\-*/])\\s*", " $1 ");
		String[] tokens = expression.trim().split("\\s+");

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

	private JsonNode getFieldValue(JsonNode data, String field) {
		String[] parts = field.split("\\.");
		JsonNode current = data;

		for (String part : parts) {
			if (current == null || current.isNull()) {
				return null;
			}

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
