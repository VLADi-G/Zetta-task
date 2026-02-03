package com.zetta.task.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class TransformationEngineTest {

	private TransformationEngine transformationEngine;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		transformationEngine = new TransformationEngine(objectMapper);
	}

	@Test
	void testSetField_SimpleFieldUpdate_UpdatesSuccessfully() throws Exception {
		String transformationJson = """
				{
				  "rules": [
				    "set data.user.age = 30"
				  ]
				}
				""";

		String dataJson = """
				{
				  "data": {
				    "user": {
				      "age": 25,
				      "name": "John"
				    }
				  }
				}
				""";

		JsonNode transformation = objectMapper.readTree(transformationJson);
		JsonNode data = objectMapper.readTree(dataJson);

		JsonNode result = transformationEngine.apply(transformation, data);

		assertEquals(30, result.get("data").get("user").get("age").asInt());
		assertEquals("John", result.get("data").get("user").get("name").asText());
	}

	@Test
	void testSetField_CreateNewField_CreatesSuccessfully() throws Exception {
		String transformationJson = "{\"rules\": [\"set data.user.fullName = data.user.firstName + \\\" \\\" + data.user.lastName\"]}";

		String dataJson = """
				{
				  "data": {
				    "user": {
				      "firstName": "John",
				      "lastName": "Doe"
				    }
				  }
				}
				""";

		JsonNode transformation = objectMapper.readTree(transformationJson);
		JsonNode data = objectMapper.readTree(dataJson);

		JsonNode result = transformationEngine.apply(transformation, data);

		assertEquals("John Doe", result.get("data").get("user").get("fullName").asText());
	}

	@Test
	void testRemoveField_ExistingField_RemovesSuccessfully() throws Exception {
		String transformationJson = """
				{
				  "rules": [
				    "remove data.user.tempField"
				  ]
				}
				""";

		String dataJson = """
				{
				  "data": {
				    "user": {
				      "name": "John",
				      "tempField": "temporary"
				    }
				  }
				}
				""";

		JsonNode transformation = objectMapper.readTree(transformationJson);
		JsonNode data = objectMapper.readTree(dataJson);

		JsonNode result = transformationEngine.apply(transformation, data);

		assertFalse(result.get("data").get("user").has("tempField"));
		assertTrue(result.get("data").get("user").has("name"));
	}

	@Test
	void testArithmeticOperations_Addition_CalculatesCorrectly() throws Exception {
		String transformationJson = "{\"rules\": [\"set data.user.age = data.user.age + 1\"]}";

		String dataJson = """
				{
				  "data": {
				    "user": {
				      "age": 25
				    }
				  }
				}
				""";

		JsonNode transformation = objectMapper.readTree(transformationJson);
		JsonNode data = objectMapper.readTree(dataJson);

		JsonNode result = transformationEngine.apply(transformation, data);

		assertEquals(26.0, result.get("data").get("user").get("age").asDouble());
	}

	@Test
	void testArithmeticOperations_Multiplication_CalculatesCorrectly() throws Exception {
		String transformationJson = """
				{
				  "rules": [
				    "set data.product.price = data.product.basePrice * 2"
				  ]
				}
				""";

		String dataJson = """
				{
				  "data": {
				    "product": {
				      "basePrice": 10.5
				    }
				  }
				}
				""";

		JsonNode transformation = objectMapper.readTree(transformationJson);
		JsonNode data = objectMapper.readTree(dataJson);

		JsonNode result = transformationEngine.apply(transformation, data);

		assertEquals(21.0, result.get("data").get("product").get("price").asDouble());
	}

	@Test
	void testMultipleTransformations_AppliesInOrder() throws Exception {
		String transformationJson = "{\"rules\": [\"set data.user.age = data.user.age + 1\", \"set data.user.fullName = data.user.firstName + \\\" \\\" + data.user.lastName\", \"remove data.user.tempData\"]}";

		String dataJson = """
				{
				  "data": {
				    "user": {
				      "firstName": "Jane",
				      "lastName": "Smith",
				      "age": 30,
				      "tempData": "remove-this"
				    }
				  }
				}
				""";

		JsonNode transformation = objectMapper.readTree(transformationJson);
		JsonNode data = objectMapper.readTree(dataJson);

		JsonNode result = transformationEngine.apply(transformation, data);

		assertEquals(31.0, result.get("data").get("user").get("age").asDouble());
		assertEquals("Jane Smith", result.get("data").get("user").get("fullName").asText());
		assertFalse(result.get("data").get("user").has("tempData"));
	}

	@Test
	void testNestedFieldAccess_DeepNesting_AccessesCorrectly() throws Exception {
		String transformationJson = """
				{
				  "rules": [
				    "set data.level1.level2.level3.value = 100"
				  ]
				}
				""";

		String dataJson = """
				{
				  "data": {
				    "level1": {
				      "level2": {
				        "level3": {
				          "value": 50
				        }
				      }
				    }
				  }
				}
				""";

		JsonNode transformation = objectMapper.readTree(transformationJson);
		JsonNode data = objectMapper.readTree(dataJson);

		JsonNode result = transformationEngine.apply(transformation, data);

		assertEquals(100, result.get("data").get("level1").get("level2").get("level3").get("value").asInt());
	}
}
