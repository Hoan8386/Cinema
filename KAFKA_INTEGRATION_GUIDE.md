# Kafka Integration Guide - Cinema Microservices

## Mục Lục

- [1. Tổng Quan](#1-tổng-quan)
- [2. Cài Đặt và Cấu Hình Kafka](#2-cài-đặt-và-cấu-hình-kafka)
- [3. Cấu Hình Common Service](#3-cấu-hình-common-service)
- [4. Cấu Hình Producer (Movies Service)](#4-cấu-hình-producer-movies-service)
- [5. Cấu Hình Consumer (Notification Service)](#5-cấu-hình-consumer-notification-service)
- [6. Retry và Dead Letter Topics](#6-retry-và-dead-letter-topics)
- [7. Kafka UI và Monitoring](#7-kafka-ui-và-monitoring)
- [8. Testing và Troubleshooting](#8-testing-và-troubleshooting)

---

## 1. Tổng Quan

### 1.1. Kiến Trúc Kafka trong Dự Án

```
┌─────────────────┐         ┌──────────────┐         ┌─────────────────────┐
│  Movies Service │ ──────> │ Kafka Broker │ ──────> │ Notification Service│
│   (Producer)    │         │   Port 9094  │         │     (Consumer)      │
└─────────────────┘         └──────────────┘         └─────────────────────┘
                                    │
                                    ├─── Topic: cinema
                                    ├─── Topic: testEmail
                                    ├─── Topic: emailTemplate
                                    ├─── Topic: cinema-retry-0
                                    ├─── Topic: cinema-retry-1
                                    ├─── Topic: cinema-retry-2
                                    └─── Topic: cinema-dlt
```

### 1.2. Các Topic Sử Dụng

| Topic Name       | Mô Tả                          | Producer       | Consumer             |
| ---------------- | ------------------------------ | -------------- | -------------------- |
| `cinema`         | Message chính về cinema events | Movies Service | Notification Service |
| `testEmail`      | Test gửi email đơn giản        | Manual/Testing | Notification Service |
| `emailTemplate`  | Email với template FreeMarker  | Manual/Testing | Notification Service |
| `cinema-retry-*` | Auto-generated retry topics    | Kafka (Auto)   | Kafka (Auto)         |
| `cinema-dlt`     | Dead Letter Topic              | Kafka (Auto)   | Notification Service |

---

## 2. Cài Đặt và Cấu Hình Kafka

### 2.1. Docker Compose Configuration

**File:** `docker-kafka.yml`

```yaml
version: "3.8"

services:
  kafka:
    image: apache/kafka
    container_name: kafka
    hostname: kafka

    ports:
      - "9092:9092" # INTERNAL (Docker network) - Dành cho containers
      - "9094:9094" # EXTERNAL (Host) - Dành cho ứng dụng chạy ngoài Docker
      - "9101:9101" # JMX (optional)

    environment:
      # ===== KRaft mode (NO ZooKeeper) =====
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_NODE_ID: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093

      # ===== LISTENERS =====
      KAFKA_LISTENERS: INTERNAL://0.0.0.0:9092,EXTERNAL://0.0.0.0:9094,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka:9092,EXTERNAL://localhost:9094
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER

      # ===== Topic defaults =====
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_NUM_PARTITIONS: 3
      KAFKA_DEFAULT_REPLICATION_FACTOR: 1

      # ===== FIX __consumer_offsets (BẮT BUỘC với single broker) =====
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS: 1

      # ===== Log retention =====
      KAFKA_LOG_RETENTION_HOURS: 168

    volumes:
      - kafka_data:/var/lib/kafka/data

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui

    ports:
      - "5678:8080"

    environment:
      KAFKA_CLUSTERS_0_NAME: local-kafka
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092

    depends_on:
      - kafka

volumes:
  kafka_data:
```

### 2.2. Khởi Động Kafka

```bash
# Khởi động Kafka và Kafka UI
docker-compose -f docker-kafka.yml up -d

# Kiểm tra status
docker-compose -f docker-kafka.yml ps

# Xem logs
docker-compose -f docker-kafka.yml logs -f kafka

# Dừng Kafka
docker-compose -f docker-kafka.yml down

# Dừng và xóa data
docker-compose -f docker-kafka.yml down -v
```

### 2.3. Giải Thích Cấu Hình Listeners

**⚠️ QUAN TRỌNG:**

- **Port 9092 (INTERNAL)**:
  - Advertised listeners: `kafka:9092`
  - Dành cho: Các containers trong Docker network
  - Hostname: `kafka`

- **Port 9094 (EXTERNAL)**:
  - Advertised listeners: `localhost:9094`
  - Dành cho: Ứng dụng Spring Boot chạy **ngoài Docker** (IDE, local development)
  - Hostname: `localhost`

**Lỗi thường gặp:**

- Nếu dùng `localhost:9092` → Sẽ bị `UnknownHostException: kafka`
- Phải dùng `localhost:9094` cho các service chạy ngoài Docker

---

## 3. Cấu Hình Common Service

Common service chứa các configuration và service dùng chung cho cả Producer và Consumer.

### 3.1. Maven Dependencies

**File:** `commonservice/pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Spring Retry (Required for @RetryableTopic) -->
    <dependency>
        <groupId>org.springframework.retry</groupId>
        <artifactId>spring-retry</artifactId>
        <version>2.0.5</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 3.2. Kafka Configuration Class

**File:** `commonservice/src/main/java/com/cinema/commonservice/configuration/KafkaConfig.java`

```java
package com.cinema.commonservice.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value(value = "${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    // ==================== PRODUCER CONFIGURATION ====================

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ==================== CONSUMER CONFIGURATION ====================

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

**Giải thích:**

1. **ProducerFactory**: Tạo Kafka producer với:
   - `BOOTSTRAP_SERVERS_CONFIG`: Địa chỉ Kafka broker
   - `KEY_SERIALIZER_CLASS_CONFIG`: Serialize key thành String
   - `VALUE_SERIALIZER_CLASS_CONFIG`: Serialize value thành String

2. **KafkaTemplate**: Bean để gửi message đến Kafka

3. **ConsumerFactory**: Tạo Kafka consumer với:
   - `BOOTSTRAP_SERVERS_CONFIG`: Địa chỉ Kafka broker
   - `GROUP_ID_CONFIG`: Consumer group ID
   - `KEY_DESERIALIZER_CLASS_CONFIG`: Deserialize key từ Kafka
   - `VALUE_DESERIALIZER_CLASS_CONFIG`: Deserialize value từ Kafka

4. **KafkaListenerContainerFactory**: Factory để tạo listener containers cho `@KafkaListener`

### 3.3. Kafka Service

**File:** `commonservice/src/main/java/com/cinema/commonservice/service/KafkaService.java`

```java
package com.cinema.commonservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Gửi message đến Kafka topic
     *
     * @param topic   Tên topic
     * @param message Nội dung message
     */
    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
        log.info("Message sent to topic: {} - Content: {}", topic, message);
    }

    /**
     * Gửi message với key
     *
     * @param topic   Tên topic
     * @param key     Key của message (dùng cho partitioning)
     * @param message Nội dung message
     */
    public void sendMessageWithKey(String topic, String key, String message) {
        kafkaTemplate.send(topic, key, message);
        log.info("Message sent to topic: {} with key: {} - Content: {}", topic, key, message);
    }
}
```

---

## 4. Cấu Hình Producer (Movies Service)

### 4.1. Maven Dependencies

**File:** `movies/pom.xml`

```xml
<dependencies>
    <!-- Common Service (chứa Kafka configuration) -->
    <dependency>
        <groupId>com.cinema</groupId>
        <artifactId>commonservice</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>

    <!-- Các dependencies khác... -->
</dependencies>
```

### 4.2. Application Properties

**File:** `movies/src/main/resources/application.properties`

```properties
spring.application.name=movies
server.port=9001

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9094
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer

# Các cấu hình khác...
```

**⚠️ Lưu ý:**

- Sử dụng port `9094` (EXTERNAL listener) cho ứng dụng chạy ngoài Docker
- **KHÔNG** sử dụng port `9092` (INTERNAL listener)

### 4.3. Controller với Producer

**File:** `movies/src/main/java/com/cinema/movies/command/controller/CinemaCommandController.java`

```java
package com.cinema.movies.command.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.cinema.commonservice.service.KafkaService;

@RestController
@RequestMapping("/api/v1/cinemas")
public class CinemaCommandController {

    @Autowired
    private KafkaService kafkaService;

    /**
     * Endpoint để gửi message đến Kafka
     * POST http://localhost:9001/api/v1/cinemas/sendMessage
     * Body: "Hello from Movies Service"
     */
    @PostMapping("/sendMessage")
    public String sendMessage(@RequestBody String message) {
        kafkaService.sendMessage("cinema", message);
        return "Message sent successfully!";
    }

    /**
     * Endpoint để gửi email test
     */
    @PostMapping("/sendTestEmail")
    public String sendTestEmail(@RequestBody String email) {
        kafkaService.sendMessage("testEmail", email);
        return "Email request sent to Kafka!";
    }

    /**
     * Endpoint để gửi email với template
     */
    @PostMapping("/sendTemplateEmail")
    public String sendTemplateEmail(@RequestBody String email) {
        kafkaService.sendMessage("emailTemplate", email);
        return "Template email request sent to Kafka!";
    }
}
```

### 4.4. Application Main Class

**File:** `movies/src/main/java/com/cinema/movies/MoviesApplication.java`

```java
package com.cinema.movies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({
    "com.cinema.movies",
    "com.cinema.commonservice"  // Scan commonservice để load KafkaConfig và KafkaService
})
public class MoviesApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoviesApplication.class, args);
    }
}
```

**⚠️ Quan trọng:** Phải có `@ComponentScan` để Spring Boot scan và load các bean từ `commonservice`.

---

## 5. Cấu Hình Consumer (Notification Service)

### 5.1. Maven Dependencies

**File:** `notificationservice/pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Common Service (chứa Kafka configuration) -->
    <dependency>
        <groupId>com.cinema</groupId>
        <artifactId>commonservice</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>

    <!-- Spring Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Spring Retry (Required for @RetryableTopic) -->
    <dependency>
        <groupId>org.springframework.retry</groupId>
        <artifactId>spring-retry</artifactId>
        <version>2.0.5</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 5.2. Application Properties

**File:** `notificationservice/src/main/resources/application.properties`

```properties
spring.application.name=notificationservice
server.port=9003

# Kafka Consumer Configuration
spring.kafka.bootstrap-servers=localhost:9094
spring.kafka.consumer.group-id=cinema

# Axon Server (nếu có)
axon.axonserver.servers=localhost:8124
```

**Giải thích:**

- `bootstrap-servers`: Địa chỉ Kafka broker (port 9094 cho EXTERNAL)
- `consumer.group-id`: Consumer group ID để quản lý offset và load balancing

### 5.3. Application Main Class

**File:** `notificationservice/src/main/java/com/cinema/notificationservice/NotificationserviceApplication.java`

```java
package com.cinema.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka  // Enable Kafka listeners
@ComponentScan({
    "com.cinema.notificationservice",
    "com.cinema.commonservice"  // Scan commonservice để load KafkaConfig
})
public class NotificationserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationserviceApplication.class, args);
    }
}
```

**Lưu ý:**

- `@EnableKafka`: Bật Kafka listeners (optional với Spring Boot auto-configuration, nhưng recommended để rõ ràng)
- `@ComponentScan`: Scan commonservice để load Kafka configuration

### 5.4. Event Consumer

**File:** `notificationservice/src/main/java/com/cinema/notificationservice/event/EventConsumer.java`

```java
package com.cinema.notificationservice.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.cinema.commonservice.service.EmailService;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class EventConsumer {

    @Autowired
    private EmailService emailService;

    /**
     * Consumer cho topic "cinema" với retry mechanism
     *
     * Flow: cinema -> cinema-retry-0 -> cinema-retry-1 -> cinema-retry-2 -> cinema-dlt
     */
    @RetryableTopic(
        attempts = "4",  // 1 lần đầu + 3 lần retry = 4 attempts
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        include = { RetriableException.class, RuntimeException.class }
    )
    @KafkaListener(topics = "cinema", containerFactory = "kafkaListenerContainerFactory")
    public void listen(String message) {
        log.info("✅ Received message from topic 'cinema': {}", message);

        // Xử lý business logic
        processMessage(message);

        // Uncomment để test retry mechanism
        // throw new RuntimeException("Error test");
    }

    /**
     * Dead Letter Topic handler
     * Nhận message khi đã retry max attempts mà vẫn fail
     */
    @DltHandler
    void processDltMessage(@Payload String message) {
        log.error("❌ DLT receive message (failed after all retries): {}", message);

        // Xử lý message lỗi: log to database, send alert, etc.
    }

    /**
     * Consumer cho topic "testEmail"
     */
    @KafkaListener(topics = "testEmail", containerFactory = "kafkaListenerContainerFactory")
    public void testEmail(String message) {
        log.info("📧 Received email request: {}", message);

        String template = "<div>\n" +
                "    <h1>Welcome, %s!</h1>\n" +
                "    <p>Thank you for joining us. We're excited to have you on board.</p>\n" +
                "    <p>Your username is: <strong>%s</strong></p>\n" +
                "</div>";
        String filledTemplate = String.format(template, "Cinema User", message);

        emailService.sendEmail(message, "Welcome to Cinema", filledTemplate, true, null);
        log.info("✅ Email sent successfully to: {}", message);
    }

    /**
     * Consumer cho topic "emailTemplate" với FreeMarker template
     */
    @KafkaListener(topics = "emailTemplate", containerFactory = "kafkaListenerContainerFactory")
    public void emailTemplate(String message) {
        log.info("📧 Received template email request: {}", message);

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("name", "Cinema Premium User");
        placeholders.put("cinemaName", "CGV Vincom");

        emailService.sendEmailWithTemplate(
            message,
            "Welcome to Cinema",
            "emailTemplate.ftl",
            placeholders,
            null
        );
        log.info("✅ Template email sent successfully to: {}", message);
    }

    /**
     * Business logic processing
     */
    private void processMessage(String message) {
        // Implement business logic here
        log.debug("Processing message: {}", message);
    }
}
```

---

## 6. Retry và Dead Letter Topics

### 6.1. Retry Mechanism

Annotation `@RetryableTopic` cung cấp retry mechanism tự động:

```java
@RetryableTopic(
    attempts = "4",                    // Tổng số lần thử (1 lần đầu + 3 retry)
    autoCreateTopics = "true",         // Tự động tạo retry topics
    dltStrategy = DltStrategy.FAIL_ON_ERROR,  // Strategy cho DLT
    include = { RetriableException.class, RuntimeException.class }  // Exceptions cần retry
)
```

### 6.2. Flow Retry

```
Message gửi đến topic "cinema"
         ↓
    (Attempt 1) Failed
         ↓
    cinema-retry-0
         ↓
    (Attempt 2) Failed
         ↓
    cinema-retry-1
         ↓
    (Attempt 3) Failed
         ↓
    cinema-retry-2
         ↓
    (Attempt 4) Failed
         ↓
    cinema-dlt (Dead Letter Topic)
         ↓
    @DltHandler processDltMessage()
```

### 6.3. Dead Letter Topic Handler

```java
@DltHandler
void processDltMessage(@Payload String message) {
    log.error("❌ Message failed after all retries: {}", message);

    // Xử lý message lỗi:
    // 1. Log vào database
    // 2. Gửi alert đến admin
    // 3. Store để manual review
    // 4. Send notification
}
```

### 6.4. Custom Retry Configuration

Để custom retry delay, backoff, v.v.:

```java
@RetryableTopic(
    attempts = "5",
    backoff = @Backoff(delay = 1000, multiplier = 2.0),  // 1s, 2s, 4s, 8s
    autoCreateTopics = "true",
    include = { RetriableException.class },
    exclude = { NullPointerException.class }  // Không retry cho NPE
)
```

---

## 7. Kafka UI và Monitoring

### 7.1. Truy Cập Kafka UI

Kafka UI đã được cấu hình trong `docker-kafka.yml`:

- **URL**: http://localhost:5678
- **Username/Password**: Không cần (default)

### 7.2. Các Chức Năng Kafka UI

1. **Topics Management**:
   - View tất cả topics
   - Xem messages trong topic
   - Xem partitions và replicas
   - Tạo/xóa topics

2. **Consumer Groups**:
   - View consumer groups
   - Xem offset của từng partition
   - Xem lag (số message chưa consume)

3. **Brokers**:
   - View broker information
   - Xem config của broker
   - Monitor broker health

4. **Messages**:
   - Send message manually
   - View message content
   - Search messages

### 7.3. Monitoring Topics

Các topics được tự động tạo:

| Topic Name       | Partitions | Purpose                      |
| ---------------- | ---------- | ---------------------------- |
| `cinema`         | 3          | Main topic cho cinema events |
| `testEmail`      | 3          | Test email sending           |
| `emailTemplate`  | 3          | Email với template           |
| `cinema-retry-0` | Auto       | First retry attempt          |
| `cinema-retry-1` | Auto       | Second retry attempt         |
| `cinema-retry-2` | Auto       | Third retry attempt          |
| `cinema-dlt`     | Auto       | Dead Letter Topic            |

---

## 8. Testing và Troubleshooting

### 8.1. Test Producer (Movies Service)

#### Test 1: Gửi Message Đơn Giản

**Endpoint:** `POST http://localhost:9001/api/v1/cinemas/sendMessage`

**Request:**

```http
POST http://localhost:9001/api/v1/cinemas/sendMessage
Content-Type: text/plain

Hello from Movies Service! This is a test message.
```

**Expected Response:**

```json
"Message sent successfully!"
```

**Expected Logs (Movies Service):**

```
INFO c.c.commonservice.service.KafkaService : Message sent to topic: cinema - Content: Hello from Movies Service! This is a test message.
```

**Expected Logs (Notification Service):**

```
INFO c.c.n.event.EventConsumer : ✅ Received message from topic 'cinema': Hello from Movies Service! This is a test message.
```

#### Test 2: Gửi Test Email

**Endpoint:** `POST http://localhost:9001/api/v1/cinemas/sendTestEmail`

**Request:**

```http
POST http://localhost:9001/api/v1/cinemas/sendTestEmail
Content-Type: text/plain

user@example.com
```

#### Test 3: Gửi Template Email

**Endpoint:** `POST http://localhost:9001/api/v1/cinemas/sendTemplateEmail`

**Request:**

```http
POST http://localhost:9001/api/v1/cinemas/sendTemplateEmail
Content-Type: text/plain

premium-user@example.com
```

### 8.2. Test Retry Mechanism

Để test retry, uncomment dòng throw exception trong `EventConsumer`:

```java
@KafkaListener(topics = "cinema", containerFactory = "kafkaListenerContainerFactory")
public void listen(String message) {
    log.info("✅ Received message: {}", message);
    throw new RuntimeException("Error test");  // Uncomment để test
}
```

**Expected Flow:**

1. Message nhận ở topic `cinema` → Failed
2. Message retry ở `cinema-retry-0` → Failed
3. Message retry ở `cinema-retry-1` → Failed
4. Message retry ở `cinema-retry-2` → Failed
5. Message chuyển vào `cinema-dlt`
6. `@DltHandler` xử lý message

### 8.3. Troubleshooting

#### Problem 1: UnknownHostException: kafka

**Error:**

```
java.net.UnknownHostException: kafka
```

**Cause:** Đang sử dụng port 9092 (INTERNAL listener) thay vì 9094 (EXTERNAL listener)

**Solution:**

```properties
# ❌ SAI
spring.kafka.bootstrap-servers=localhost:9092

# ✅ ĐÚNG
spring.kafka.bootstrap-servers=localhost:9094
```

#### Problem 2: Consumer Không Nhận Message

**Checklist:**

1. ✅ Kafka broker có đang chạy không?

   ```bash
   docker ps | grep kafka
   ```

2. ✅ Cấu hình `bootstrap-servers` đúng chưa?

   ```properties
   spring.kafka.bootstrap-servers=localhost:9094
   ```

3. ✅ Consumer group-id được cấu hình chưa?

   ```properties
   spring.kafka.consumer.group-id=cinema
   ```

4. ✅ `@ComponentScan` có scan `commonservice` chưa?

   ```java
   @ComponentScan({"com.cinema.notificationservice", "com.cinema.commonservice"})
   ```

5. ✅ `@EnableKafka` annotation có được thêm chưa?

   ```java
   @EnableKafka
   ```

6. ✅ Dependency `spring-kafka` có trong pom.xml chưa?

#### Problem 3: Build Failed - Unknown Property

**Error:**

```
'spring.kafka.bootstrap-servers' is an unknown property.
```

**Cause:** Thiếu dependency `spring-kafka` trong service

**Solution:** Thêm dependency vào `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

#### Problem 4: Message Không Gửi Được

**Checklist:**

1. ✅ Xem logs của Movies Service có message "Message sent to topic" không
2. ✅ Check Kafka UI xem message có trong topic không
3. ✅ Check consumer lag trong Kafka UI
4. ✅ Restart cả Movies và Notification services

#### Problem 5: Retry Topic Không Hoạt Động

**Checklist:**

1. ✅ Dependency `spring-retry` có trong pom.xml chưa?

   ```xml
   <dependency>
       <groupId>org.springframework.retry</groupId>
       <artifactId>spring-retry</artifactId>
       <version>2.0.5</version>
   </dependency>
   ```

2. ✅ `autoCreateTopics = "true"` trong `@RetryableTopic`

3. ✅ Exception có match với `include` configuration không

### 8.4. Useful Commands

#### Docker Commands

```bash
# Xem logs của Kafka
docker logs -f kafka

# Xem logs của Kafka UI
docker logs -f kafka-ui

# Restart Kafka
docker restart kafka

# Exec vào Kafka container
docker exec -it kafka bash

# List topics
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Describe topic
docker exec -it kafka kafka-topics.sh --describe --topic cinema --bootstrap-server localhost:9092

# Consume messages từ terminal
docker exec -it kafka kafka-console-consumer.sh --topic cinema --from-beginning --bootstrap-server localhost:9092

# Produce messages từ terminal
docker exec -it kafka kafka-console-producer.sh --topic cinema --bootstrap-server localhost:9092
```

#### Maven Commands

```bash
# Clean và build
./mvnw clean install -DskipTests

# Build specific module
./mvnw clean install -DskipTests -pl commonservice
./mvnw clean install -DskipTests -pl movies
./mvnw clean install -DskipTests -pl notificationservice

# Run service
./mvnw spring-boot:run
```

---

## 9. Best Practices

### 9.1. Configuration Management

1. **Externalize Configuration**: Sử dụng application.properties/yml
2. **Environment Specific**: Tạo profile cho dev, staging, prod
3. **Security**: Không hardcode credentials

### 9.2. Error Handling

1. **Implement Retry**: Sử dụng `@RetryableTopic` cho transient errors
2. **DLT Handler**: Luôn implement `@DltHandler` để xử lý failed messages
3. **Logging**: Log đầy đủ thông tin để troubleshoot

### 9.3. Performance

1. **Batch Processing**: Xem xét batch consumer nếu volume lớn
2. **Concurrency**: Configure `concurrency` trong listener factory
3. **Partitioning**: Sử dụng key để distribute messages across partitions

### 9.4. Monitoring

1. **Consumer Lag**: Monitor lag thường xuyên
2. **Error Rate**: Track retry và DLT message rate
3. **Throughput**: Monitor message throughput

---

## 10. Advanced Topics

### 10.1. Custom Serializers/Deserializers

Để gửi/nhận complex objects (JSON):

```java
// Producer
configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

// Consumer
configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.cinema.*");
```

### 10.2. Transactional Messaging

Enable transactions cho exactly-once semantics:

```java
configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-cinema");
configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```

### 10.3. Custom Partitioner

Implement custom partitioner để control message distribution:

```java
public class CustomPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        // Custom partitioning logic
    }
}
```

---

## 11. Tổng Kết

### 11.1. Workflow Hoàn Chỉnh

```
1. Start Kafka:
   docker-compose -f docker-kafka.yml up -d

2. Build commonservice:
   cd commonservice
   ./mvnw clean install -DskipTests

3. Start Movies Service:
   cd movies
   ./mvnw spring-boot:run

4. Start Notification Service:
   cd notificationservice
   ./mvnw spring-boot:run

5. Test:
   POST http://localhost:9001/api/v1/cinemas/sendMessage
   Body: "Hello Kafka!"

6. Verify:
   - Check Movies logs: "Message sent to topic: cinema"
   - Check Notification logs: "Received message from topic 'cinema': Hello Kafka!"
   - Check Kafka UI: http://localhost:5678
```

### 11.2. Checklist Tích Hợp Kafka

- [ ] Kafka broker đang chạy (port 9094)
- [ ] Kafka UI accessible (port 5678)
- [ ] Dependencies đầy đủ trong tất cả services
- [ ] KafkaConfig trong commonservice
- [ ] application.properties có bootstrap-servers và group-id
- [ ] @ComponentScan scan commonservice
- [ ] @EnableKafka trong Notification service
- [ ] Producer endpoint hoạt động
- [ ] Consumer nhận được message
- [ ] Retry mechanism hoạt động
- [ ] DLT handler hoạt động

---

## 12. References

- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [KRaft Mode](https://kafka.apache.org/documentation/#kraft)
- [Kafka UI](https://github.com/provectus/kafka-ui)

---

**Tác giả:** Cinema Microservices Team  
**Ngày cập nhật:** 2026-01-19  
**Phiên bản:** 1.0.0
