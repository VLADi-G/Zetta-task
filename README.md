# Message Processing Backend

A Spring Boot application that processes messages from Kafka, applies dynamic conditions and transformations, persists data with state tracking, and publishes results to an output topic.

## Features

- **Kafka Message Consumption**: Consumes messages from a configurable input topic
- **Condition Engine**: Evaluates messages against dynamic conditions (all/any/none operators)
- **Transformation Engine**: Applies flexible transformations to message data
- **Data Persistence**: Stores message state and change history using JPA/H2
- **State Delta Tracking**: Maintains history of all state changes
- **Output Publishing**: Publishes processed messages to a configurable output topic
- **Centralized Error Handling**: Unified exception handling and structured logging
- **Clean Architecture**: Separation of concerns with dependency injection

## Prerequisites

- **Java 21** or higher
- **Maven 3.9+**
- **Docker** and **Docker Compose** (for containerized setup)

## Project Structure

```
src/
├── main/
│   ├── java/com/zetta/task/
│   │   ├── TaskApplication.java           # Spring Boot main class
│   │   ├── config/                        # Configuration classes
│   │   ├── consumer/                      # Kafka consumer
│   │   ├── producer/                      # Kafka producer
│   │   ├── engine/                        # Condition and Transformation engines
│   │   ├── model/                         # JPA entities
│   │   ├── repository/                    # Data repositories
│   │   ├── service/                       # Business logic
│   │   └── exception/                     # Exception handling
│   └── resources/
│       ├── application.yml                # Main configuration
│       ├── application-docker.yml         # Docker profile
│       ├── condition-rules.json           # Condition definitions
│       ├── transformation-rules.json      # Transformation rules
│       ├── example-input.json             # Example input message
│       └── example-output.json            # Example output message
└── test/
    └── java/com/zetta/task/               # Unit tests
```

## Configuration

All configuration is externalized in `application.yml`. Key settings include:

### Kafka Settings
```yaml
app:
  kafka:
    input-topic: input-messages           # Topic to consume from
    output-topic: output-messages         # Topic to publish to
    consumer-group: message-processor-group

spring:
  kafka:
    bootstrap-servers: localhost:9092     # Kafka broker address
```

### Database Settings
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:messagedb           # H2 in-memory database
    username: sa
    password:
```

### Rule Files
```yaml
app:
  rules:
    condition-file: classpath:condition-rules.json
    transformation-file: classpath:transformation-rules.json
```

All settings can be overridden using environment variables (e.g., `KAFKA_BOOTSTRAP_SERVERS`, `INPUT_TOPIC`, `OUTPUT_TOPIC`).

## Condition DSL

The Condition Engine supports logical operations to evaluate message data.

### Operators

- **all**: All conditions must be true (AND logic)
- **any**: At least one condition must be true (OR logic)  
- **none**: No conditions must be true (NOT logic)

### Comparison Operators

- `equals`, `notEquals`
- `greaterThan`, `lessThan`, `greaterThanOrEqual`, `lessThanOrEqual`
- `contains` (for strings)

### Example

```json
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
    },
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
  ]
}
```

This condition checks:
1. User is 18 or older
2. User is from US or CA
3. User is not blocked

## Transformation Syntax

The Transformation Engine supports dynamic data manipulation using simple expressions.

### Operations

- **Set/Update Field**: `set field.path = value`
- **Create Field**: `set field.newField = expression`
- **Remove Field**: `remove field.path`
- **Arithmetic**: `set field = field + 1` (supports +, -, *, /)
- **String Concatenation**: `set field = field1 + " " + field2`
- **Nested Field Access**: Use dot notation (e.g., `data.user.name`)

### Example

```json
{
  "rules": [
    "set data.user.age = data.user.age + 1",
    "set data.user.fullName = data.user.firstName + \" \" + data.user.lastName",
    "set data.processedAt = \"2026-02-03T10:00:00\"",
    "set data.user.isAdult = true",
    "remove data.user.tempField"
  ]
}
```

## Example Input/Output

### Input Message
```json
{
  "id": "msg-001",
  "data": {
    "user": {
      "firstName": "John",
      "lastName": "Doe",
      "age": 25,
      "country": "US",
      "status": "active",
      "tempField": "remove-me"
    }
  }
}
```

### Output Message
```json
{
  "id": "msg-001",
  "data": {
    "user": {
      "firstName": "John",
      "lastName": "Doe",
      "age": 26,
      "country": "US",
      "status": "active",
      "fullName": "John Doe",
      "isAdult": true
    },
    "processedAt": "2026-02-03T10:00:00"
  }
}
```

**Changes Applied**:
- Age incremented from 25 to 26
- Full name created by concatenating first and last name
- `processedAt` timestamp added
- `isAdult` flag set to true
- `tempField` removed

## Running Locally

### 1. Start Kafka

You need a running Kafka instance. Using Docker:

```bash
docker run -d --name zookeeper -p 2181:2181 confluentinc/cp-zookeeper:7.6.0 \
  -e ZOOKEEPER_CLIENT_PORT=2181

docker run -d --name kafka -p 9092:9092 -p 29092:29092 \
  --link zookeeper \
  confluentinc/cp-kafka:7.6.0 \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT \
  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
```

### 2. Build and Run Application

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/task-0.0.1-SNAPSHOT.jar
```

### 3. Send Test Messages

```bash
# Create topics (if not auto-created)
docker exec -it kafka kafka-topics --create --topic input-messages --bootstrap-server localhost:9092
docker exec -it kafka kafka-topics --create --topic output-messages --bootstrap-server localhost:9092

# Send a test message
docker exec -it kafka kafka-console-producer --topic input-messages --bootstrap-server localhost:9092
# Then paste the example input JSON

# Consume output messages
docker exec -it kafka kafka-console-consumer --topic output-messages --bootstrap-server localhost:9092 --from-beginning
```

## Running with Docker Compose

The easiest way to run the entire stack:

```bash
# Build and start all services
docker-compose up --build

# Send test messages
docker exec -it kafka kafka-console-producer --topic input-messages --bootstrap-server localhost:9092

# View output messages
docker exec -it kafka kafka-console-consumer --topic output-messages --bootstrap-server localhost:9092 --from-beginning

# Stop all services
docker-compose down
```

Services started:
- **Zookeeper**: Port 2181
- **Kafka**: Ports 9092 (internal), 29092 (external)
- **Message Processor**: Automatically connects to Kafka

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ConditionEngineTest

# Run with coverage
mvn clean test jacoco:report
```

## Database Access

H2 Console is available at: `http://localhost:8080/h2-console`

Connection details:
- JDBC URL: `jdbc:h2:mem:messagedb`
- Username: `sa`
- Password: (empty)

You can query:
- `MESSAGE_STATE`: Current state of processed messages
- `STATE_DELTA`: History of all state changes

## Architecture Overview

### Message Flow

1. **Consume**: `MessageConsumer` receives messages from Kafka input topic
2. **Evaluate**: `ConditionEngine` checks if message meets defined conditions
3. **Transform**: `TransformationEngine` applies transformation rules
4. **Persist**: `MessageProcessor` saves state to database via JPA repositories
5. **Track**: `StateDelta` entity stores before/after snapshots
6. **Publish**: `MessageProducer` sends transformed message to output topic

### Clean Architecture Layers

- **Presentation**: Kafka consumers/producers
- **Application**: Service layer (`MessageProcessor`)
- **Domain**: Engines (condition, transformation), business logic
- **Infrastructure**: Repositories, JPA entities, external configurations

### Error Handling

All exceptions are caught by `GlobalExceptionHandler`:
- `ConfigurationException`: Configuration/setup errors
- `ConditionException`: Condition evaluation failures
- `TransformationException`: Transformation errors
- `Exception`: Unexpected errors

All errors are logged with structured information.

## Technologies Used

- **Spring Boot 3.2.2**: Application framework
- **Java 21**: Programming language
- **Spring Kafka**: Kafka integration
- **Spring Data JPA**: Data persistence
- **H2 Database**: In-memory database
- **Jackson**: JSON processing
- **Lombok**: Reduce boilerplate code
- **JUnit 5**: Testing framework
- **Mockito**: Mocking for tests
- **Docker**: Containerization

## Development Notes

- All configuration is externalized for easy deployment
- State changes are tracked with full before/after history
- Condition and transformation rules are loaded from JSON files
- Clean separation between engines, services, and infrastructure
- Comprehensive error handling and logging throughout
- Tests cover core functionality: conditions, transformations, and message flow

## Future Enhancements

- REST API for manual message submission and state querying
- More sophisticated delta calculation (field-by-field diff)
- Support for more complex transformation operations
- Dead Letter Queue (DLQ) for failed messages
- Metrics and monitoring (Prometheus/Grafana)
- Multiple rule sets with dynamic selection

## License

This is a technical assignment project.
