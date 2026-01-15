# 🎬 Cinema API Gateway

## 📋 Mục lục

- [Tổng quan](#tổng-quan)
- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Các tính năng chính](#các-tính-năng-chính)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Cấu hình](#cấu-hình)
- [Gateway Filters](#gateway-filters)
- [Routing Configuration](#routing-configuration)
- [Service Discovery](#service-discovery)
- [Rate Limiting](#rate-limiting)
- [Cài đặt và chạy](#cài-đặt-và-chạy)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

---

## 🎯 Tổng quan

**Cinema API Gateway** là service trung tâm trong kiến trúc microservices của hệ thống Cinema. Nó đóng vai trò là **single entry point** cho tất cả client requests, cung cấp các chức năng:

- 🔐 **Authentication & Authorization**: API Key validation
- 🚦 **Rate Limiting**: Kiểm soát traffic và ngăn chặn abuse
- 🔄 **Load Balancing**: Phân phối requests đến các service instances
- 🛣️ **Dynamic Routing**: Điều hướng requests đến đúng microservices
- 📊 **Service Discovery**: Tích hợp với Eureka để tự động phát hiện services
- 🔍 **Logging & Monitoring**: Theo dõi toàn bộ traffic qua gateway

### Đặc điểm nổi bật:

- ⚡ **Non-blocking I/O**: Sử dụng Spring WebFlux (reactive stack)
- 🔌 **Pluggable**: Dễ dàng thêm/xóa filters
- 📈 **Scalable**: Có thể chạy nhiều instances
- 🎨 **Flexible**: Cấu hình routing động qua YAML/Properties

---

## 🏗️ Kiến trúc hệ thống

### Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CINEMA MICROSERVICES                             │
│                                                                          │
│                                                                          │
│  ┌─────────────┐        ┌─────────────┐        ┌─────────────┐        │
│  │   Mobile    │        │   Web App   │        │  3rd Party  │        │
│  │     App     │        │  (React/    │        │    Client   │        │
│  │             │        │   Angular)  │        │             │        │
│  └──────┬──────┘        └──────┬──────┘        └──────┬──────┘        │
│         │                      │                       │                │
│         │  HTTP/HTTPS         │                       │                │
│         │  apiKey: xxx        │                       │                │
│         └──────────────────────┴───────────────────────┘                │
│                                │                                        │
│                                ▼                                        │
│     ┌──────────────────────────────────────────────────────────┐      │
│     │                  API GATEWAY (Port 8080)                  │      │
│     │              🎯 Single Entry Point                        │      │
│     │                                                            │      │
│     │  ┌────────────────────────────────────────────────────┐  │      │
│     │  │  Filter Chain (Request Processing)                 │  │      │
│     │  │                                                      │  │      │
│     │  │  1️⃣  KeyAuthFilter                                 │  │      │
│     │  │      ├─ Check API Key existence                    │  │      │
│     │  │      └─ Validate API Key                           │  │      │
│     │  │                                                      │  │      │
│     │  │  2️⃣  RequestRateLimiter                            │  │      │
│     │  │      ├─ Check Redis for rate limit                 │  │      │
│     │  │      ├─ IP-based throttling                        │  │      │
│     │  │      └─ Return 429 if exceeded                     │  │      │
│     │  │                                                      │  │      │
│     │  │  3️⃣  LoadBalancer (via Eureka)                     │  │      │
│     │  │      ├─ Service discovery                          │  │      │
│     │  │      ├─ Health check                               │  │      │
│     │  │      └─ Round-robin distribution                   │  │      │
│     │  │                                                      │  │      │
│     │  └────────────────────────────────────────────────────┘  │      │
│     │                                                            │      │
│     │  Routes:                                                  │      │
│     │  • /api/v1/** → Movies Service                           │      │
│     │  • Future: /api/v1/bookings/** → Booking Service        │      │
│     │  • Future: /api/v1/users/** → User Service              │      │
│     └────────────────────────┬───────────────────────────────────┘      │
│                              │                                          │
│                              ▼                                          │
│     ┌──────────────────────────────────────────────────────────┐      │
│     │          SERVICE REGISTRY (Eureka - Port 8761)            │      │
│     │                                                            │      │
│     │  • Service Registration                                   │      │
│     │  • Health Check                                           │      │
│     │  • Service Discovery                                      │      │
│     │  • Load Balancer Metadata                                │      │
│     └──────────────────────────────────────────────────────────┘      │
│                                                                          │
│                                                                          │
│  ┌──────────────────────────┐    ┌──────────────────────────┐         │
│  │  REDIS (Port 6379)       │    │  MOVIES SERVICE          │         │
│  │                          │    │  (Port 8081+)            │         │
│  │  • Rate Limit Storage    │    │                          │         │
│  │  • Token Bucket Algo     │    │  ├─ Movie Management     │         │
│  │  • IP → Request Count    │    │  ├─ MinIO Integration    │         │
│  │  • TTL: Auto-expire      │    │  ├─ MySQL Database       │         │
│  │                          │    │  └─ REST APIs            │         │
│  └──────────────────────────┘    └──────────────────────────┘         │
│                                                                          │
│                                                                          │
│  Future Services:                                                       │
│  ┌──────────────────────────┐    ┌──────────────────────────┐         │
│  │  BOOKING SERVICE         │    │  USER SERVICE            │         │
│  │  • Seat Selection        │    │  • Authentication        │         │
│  │  • Payment               │    │  • User Profile          │         │
│  │  • Ticket Generation     │    │  • Preferences           │         │
│  └──────────────────────────┘    └──────────────────────────┘         │
│                                                                          │
└────────────────────────────────────────────────────────────────────────┘
```

### Request Flow Diagram

```
CLIENT REQUEST
     │
     │ GET http://localhost:8080/api/v1/movies
     │ Header: apiKey=tranminhkhue
     ▼
┌─────────────────────────────────────┐
│   1. API GATEWAY RECEIVES REQUEST   │
│      - Port 8080                    │
│      - Parse headers & path         │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   2. ROUTE MATCHING                 │
│      - Pattern: /api/v1/**          │
│      - Matched route: "movies"      │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   3. FILTER CHAIN EXECUTION         │
│                                     │
│   ┌─────────────────────────────┐ │
│   │ 3.1 KeyAuthFilter           │ │
│   │  ✅ Check apiKey header     │ │
│   │  ✅ Validate value          │ │
│   │  ❌ 401/403 if invalid      │ │
│   └──────────┬──────────────────┘ │
│              │ PASS                │
│              ▼                      │
│   ┌─────────────────────────────┐ │
│   │ 3.2 RequestRateLimiter      │ │
│   │  • Get client IP            │ │
│   │  • Check Redis counter      │ │
│   │  • 10 req/sec limit         │ │
│   │  • 20 burst capacity        │ │
│   │  ❌ 429 if exceeded         │ │
│   └──────────┬──────────────────┘ │
│              │ PASS                │
└──────────────┼─────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   4. SERVICE DISCOVERY              │
│      - Query Eureka for "movies"    │
│      - Get available instances      │
│      - Health check                 │
└──────────────┬──────────────────────┘
               │
               │ Found: movies:8081
               ▼
┌─────────────────────────────────────┐
│   5. LOAD BALANCING                 │
│      - Select instance (Round-robin)│
│      - URI: http://movies:8081      │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   6. FORWARD TO BACKEND SERVICE     │
│      POST http://movies:8081/api/v1/movies
│      (Original headers preserved)   │
└──────────────┬──────────────────────┘
               │
               │ 200 OK + JSON Response
               ▼
┌─────────────────────────────────────┐
│   7. RETURN RESPONSE TO CLIENT      │
│      - Status: 200                  │
│      - Body: Movie list JSON        │
│      - Headers: Content-Type, etc   │
└─────────────────────────────────────┘
```

---

## ✨ Các tính năng chính

### 1. 🔐 API Authentication

- **KeyAuthFilter**: Custom filter kiểm tra API Key
- Header-based authentication
- Centralized security tại gateway
- Dễ dàng thay đổi chiến lược authentication

### 2. 🚦 Rate Limiting

- **Redis-backed**: Persistent rate limit counters
- **IP-based**: Theo dõi từng client IP
- **Token Bucket Algorithm**:
  - Replenish Rate: 10 requests/second
  - Burst Capacity: 20 requests (tối đa tích lũy)
  - Requested Tokens: 1 per request
- Tự động trả về `429 Too Many Requests`

### 3. 🔄 Dynamic Routing

- Routing based on path patterns
- Load balancing với Eureka
- URI format: `lb://service-name`
- Không hard-code service addresses

### 4. 📊 Service Discovery

- Tích hợp Netflix Eureka Client
- Tự động đăng ký với Eureka Server
- Health check định kỳ
- Failover tự động

### 5. ⚡ Reactive Programming

- Non-blocking I/O với Project Reactor
- WebFlux stack (không phải Servlet)
- Xử lý concurrent requests hiệu quả
- Backpressure handling

### 6. 🔍 Observability

- DEBUG logging cho gateway operations
- Request/Response logging
- Error tracking
- Performance metrics ready

---

## 🛠️ Công nghệ sử dụng

### Core Framework

- **Spring Boot**: 3.2.1
- **Spring Cloud**: 2023.0.0
- **Java**: 17 (LTS)

### Key Dependencies

| Dependency                                 | Version  | Mục đích                                     |
| ------------------------------------------ | -------- | -------------------------------------------- |
| spring-cloud-starter-gateway               | 2023.0.0 | Core gateway functionality, routing, filters |
| spring-cloud-starter-netflix-eureka-client | 2023.0.0 | Service registration & discovery             |
| spring-boot-starter-data-redis-reactive    | 3.2.1    | Reactive Redis client cho rate limiting      |
| spring-boot-starter-test                   | 3.2.1    | Testing utilities                            |

### Infrastructure

- **Redis**: 6.x+ (Rate limiting storage)
- **Eureka Server**: Service registry
- **Maven**: Build tool

---

## 📁 Cấu trúc dự án

```
apigateway/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cinema/apigateway/
│   │   │       ├── ApigatewayApplication.java       # 🚀 Main application
│   │   │       └── Filter/
│   │   │           └── KeyAuthFilter.java           # 🔐 API Key validation
│   │   │
│   │   └── resources/
│   │       ├── application.properties                # 🔧 Properties config
│   │       └── application.yml                      # 🔧 YAML config (routing)
│   │
│   └── test/
│       └── java/
│           └── com/cinema/apigateway/
│               └── ApigatewayApplicationTests.java  # ✅ Unit tests
│
├── target/                                          # 📦 Build output
│   ├── classes/                                     # Compiled classes
│   └── apigateway-0.0.1-SNAPSHOT.jar.original      # Executable JAR
│
├── pom.xml                                          # 📋 Maven configuration
├── mvnw, mvnw.cmd                                   # 🔨 Maven wrapper
└── README.md                                        # 📖 This file
```

### Mô tả các file quan trọng:

#### `ApigatewayApplication.java`

```java
@SpringBootApplication
public class ApigatewayApplication {
    // Main entry point

    @Bean
    public KeyResolver userKeyResolver() {
        // Định nghĩa cách resolve key cho rate limiting
        // Hiện tại: dùng client IP address
        return exchange -> Mono.just(
            exchange.getRequest()
                   .getRemoteAddress()
                   .getAddress()
                   .getHostAddress()
        );
    }
}
```

#### `KeyAuthFilter.java`

- Custom GatewayFilterFactory
- Extends `AbstractGatewayFilterFactory`
- Authentication logic
- Error handling & response formatting

#### `application.yml`

- Route definitions
- Filter configurations
- Eureka client settings
- Logging levels

#### `application.properties`

- Application name
- API Key configuration
- Simple key-value configs

---

## ⚙️ Cấu hình

### 1. Application Properties (`application.properties`)

```properties
# Application Identity
spring.application.name=apigateway

# Security Configuration
apiKey=tranminhkhue
```

**Best Practice cho Production:**

```properties
# Sử dụng environment variable
apiKey=${API_KEY:default-dev-key}
```

```bash
# Set trong environment
export API_KEY=super-secure-production-key
```

### 2. Application YAML (`application.yml`)

```yaml
# Server Configuration
server:
  port: 8080 # Gateway port

spring:
  application:
    name: gateway-service # Service name trong Eureka

  # Redis Configuration (cho Rate Limiting)
  data:
    redis:
      port: 6379
      host: localhost
      # password: ${REDIS_PASSWORD}  # Nếu có password
      # database: 0                   # Redis database index

  # Cloud Gateway Configuration
  cloud:
    gateway:
      # Service Discovery Integration
      discovery:
        locator:
          enabled: true # Tự động tạo routes từ Eureka services

      # Routes Definition
      routes:
        - id: movies # Route identifier (unique)
          uri: lb://movies # Load-balanced URI (service name từ Eureka)
          predicates:
            - Path=/api/v1/** # URL pattern matching
          filters:
            # Custom Authentication Filter
            - name: KeyAuthFilter

            # Built-in Rate Limiter
            - name: RequestRateLimiter
              args:
                # Token Bucket Algorithm Parameters
                redis-rate-limiter.replenishRate: 10 # 10 tokens/second
                redis-rate-limiter.burstCapacity: 20 # Max 20 tokens
                redis-rate-limiter.requestedTokens: 1 # 1 token per request

# Eureka Client Configuration
eureka:
  instance:
    name: localhost
    # preferIpAddress: true       # Đăng ký bằng IP thay vì hostname
    # leaseRenewalIntervalInSeconds: 10  # Heartbeat interval
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka # Eureka server URL
    register-with-eureka: true # Đăng ký gateway như một service
    fetch-registry: true # Fetch danh sách services

# Logging Configuration
logging:
  level:
    org.springframework.cloud.gateway: DEBUG # Gateway debug logs
    # com.cinema.apigateway: DEBUG            # Application logs
```

### 3. Cấu hình nâng cao

#### Thêm CORS Configuration

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          "[/**]":
            allowedOrigins:
              - "http://localhost:3000"
              - "https://cinema-app.com"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            exposedHeaders:
              - apiKey
            allowCredentials: true
            maxAge: 3600
```

#### Multiple Routes Example

```yaml
routes:
  # Movies Service
  - id: movies
    uri: lb://movies
    predicates:
      - Path=/api/v1/movies/**
    filters:
      - name: KeyAuthFilter
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 10
          redis-rate-limiter.burstCapacity: 20

  # Booking Service (Future)
  - id: bookings
    uri: lb://booking-service
    predicates:
      - Path=/api/v1/bookings/**
    filters:
      - name: KeyAuthFilter
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 5
          redis-rate-limiter.burstCapacity: 10

  # Public Health Check (No Authentication)
  - id: health
    uri: lb://movies
    predicates:
      - Path=/health
    filters:
      - AddResponseHeader=X-Response-Type, Health
```

---

## 🔧 Gateway Filters

### Built-in Filters

#### 1. RequestRateLimiter

**Mục đích**: Giới hạn số lượng requests từ mỗi client

**Cấu hình**:

```yaml
- name: RequestRateLimiter
  args:
    redis-rate-limiter.replenishRate: 10 # Tokens được thêm/giây
    redis-rate-limiter.burstCapacity: 20 # Số tokens tối đa
    redis-rate-limiter.requestedTokens: 1 # Tokens cần cho 1 request
```

**Algorithm**: Token Bucket

- Client bắt đầu với `burstCapacity` tokens
- Mỗi request tiêu tốn `requestedTokens`
- Tokens được nạp lại với tốc độ `replenishRate`/giây
- Nếu không đủ tokens → 429 Too Many Requests

**KeyResolver**: Định nghĩa trong `ApigatewayApplication.java`

```java
@Bean
public KeyResolver userKeyResolver() {
    // IP-based: mỗi IP có rate limit riêng
    return exchange -> Mono.just(
        exchange.getRequest()
               .getRemoteAddress()
               .getAddress()
               .getHostAddress()
    );
}
```

**Response khi exceeded:**

```json
{
  "status": 429,
  "error": "Too Many Requests"
}
```

#### 2. Các Built-in Filters khác (có thể thêm)

```yaml
# Add Request Header
- AddRequestHeader=X-Request-Source, API-Gateway

# Add Response Header
- AddResponseHeader=X-Response-Time, ${responseTime}

# Rewrite Path
- RewritePath=/api/v1/(?<segment>.*), /${segment}

# Retry
- name: Retry
  args:
    retries: 3
    statuses: BAD_GATEWAY

# Circuit Breaker (with Resilience4j)
- name: CircuitBreaker
  args:
    name: moviesCircuitBreaker
    fallbackUri: forward:/fallback/movies
```

### Custom Filters

#### KeyAuthFilter (Đã implement)

**Location**: `com.cinema.apigateway.Filter.KeyAuthFilter`

**Chức năng**:

1. Kiểm tra header `apiKey` có tồn tại
2. Validate giá trị với config
3. Trả về 401 nếu thiếu, 403 nếu sai

**Configuration**:

```yaml
filters:
  - name: KeyAuthFilter # Tên class đầy đủ
```

**Code Flow**:

```java
1. Check header exists → Không → 401 UNAUTHORIZED
2. Extract value → Compare với @Value("${apiKey}")
3. Match → chain.filter() → Không match → 403 FORBIDDEN
```

**Error Response Format**:

```json
{
  "timestamp": "2026-01-15T10:30:00.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing authorization information",
  "path": "/api/v1/movies"
}
```

#### Cách tạo Custom Filter mới

**Template**:

```java
@Component
public class CustomLogFilter extends AbstractGatewayFilterFactory<CustomLogFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(CustomLogFilter.class);

    public CustomLogFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            String path = exchange.getRequest().getURI().getPath();

            log.info("Request started: {}", path);

            return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Request completed: {} in {}ms", path, duration);
                });
        };
    }

    public static class Config {
        // Configuration properties
    }
}
```

**Sử dụng**:

```yaml
filters:
  - name: CustomLogFilter
```

---

## 🛣️ Routing Configuration

### Route Anatomy

```yaml
routes:
  - id: unique-route-id # Identifier (unique trong gateway)
    uri: lb://service-name # Destination URI
    predicates: # Matching conditions (AND logic)
      - Path=/api/v1/**
      - Method=GET,POST
      - Header=X-Request-Type, Mobile
    filters: # Filters to apply
      - name: KeyAuthFilter
    metadata: # Optional metadata
      requestTimeout: 5000
```

### URI Schemes

```yaml
# Load-balanced (via Eureka)
uri: lb://movies                 # ✅ Recommended

# Direct HTTP
uri: http://localhost:8081       # ❌ Hard-coded, không scale

# HTTPS
uri: https://external-api.com

# WebSocket
uri: lb:ws://websocket-service
```

### Predicates Reference

```yaml
predicates:
  # Path Matching
  - Path=/api/v1/** # Wildcard
  - Path=/api/{version}/** # Path variable

  # HTTP Method
  - Method=GET,POST

  # Headers
  - Header=X-Request-Id, \d+ # Header với regex

  # Query Parameters
  - Query=debug, true

  # Host
  - Host=**.cinema.com

  # Time-based
  - After=2026-01-01T00:00:00Z
  - Before=2026-12-31T23:59:59Z
  - Between=2026-01-01T00:00:00Z, 2026-12-31T23:59:59Z

  # Cookie
  - Cookie=session, [A-Za-z0-9]+

  # Remote Address (IP)
  - RemoteAddr=192.168.1.0/24
```

### Route Examples

#### Public Route (No Auth)

```yaml
- id: health-check
  uri: lb://movies
  predicates:
    - Path=/health
    - Method=GET
  filters:
    - AddResponseHeader=X-Health-Check, true
```

#### Authenticated Route

```yaml
- id: movies-protected
  uri: lb://movies
  predicates:
    - Path=/api/v1/movies/**
  filters:
    - name: KeyAuthFilter
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 10
        redis-rate-limiter.burstCapacity: 20
```

#### Admin Route (Higher Rate Limit)

```yaml
- id: admin-api
  uri: lb://admin-service
  predicates:
    - Path=/admin/**
    - Header=X-Admin-Token
  filters:
    - name: KeyAuthFilter
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 100
        redis-rate-limiter.burstCapacity: 200
```

---

## 📡 Service Discovery

### Eureka Integration

**Configuration**:

```yaml
eureka:
  instance:
    name: localhost
    preferIpAddress: true # Dùng IP thay vì hostname
    leaseRenewalIntervalInSeconds: 10 # Heartbeat mỗi 10 giây
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    register-with-eureka: true # Gateway cũng đăng ký như một service
    fetch-registry: true # Lấy danh sách services
    registry-fetch-interval-seconds: 10 # Refresh mỗi 10 giây
```

### Service Name Resolution

**Format**: `lb://<service-name>`

```yaml
uri: lb://movies
```

**Process**:

1. Gateway query Eureka: "Where is service 'movies'?"
2. Eureka response: "movies có 2 instances: 192.168.1.10:8081, 192.168.1.11:8081"
3. Gateway load balance: chọn 1 instance (round-robin)
4. Forward request đến instance đó

### Load Balancing Strategies

**Default**: Round-robin

**Custom Load Balancer** (optional):

```java
@Configuration
public class LoadBalancerConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {

        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
            loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
            name
        );
    }
}
```

### Health Check

Gateway tự động check health của services qua Eureka:

- Services gửi heartbeat mỗi 30 giây (default)
- Nếu miss 3 heartbeats → marked as DOWN
- Gateway không route đến DOWN instances

---

## 🚦 Rate Limiting

### Token Bucket Algorithm

**Concept**:

```
Bucket (Capacity: 20)
├─ Token: 🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙🪙
│
├─ Request arrives → Consume 1 token: 🪙
│  ├─ Token available → ✅ Allow request
│  └─ No token → ❌ Reject (429)
│
└─ Refill: +10 tokens/second (replenishRate)
```

### Configuration Parameters

```yaml
redis-rate-limiter.replenishRate: 10 # Tokens per second
redis-rate-limiter.burstCapacity: 20 # Max tokens in bucket
redis-rate-limiter.requestedTokens: 1 # Tokens per request
```

**Examples**:

| Config                                 | Meaning                         |
| -------------------------------------- | ------------------------------- |
| replenishRate: 10, burstCapacity: 20   | 10 req/s steady, burst up to 20 |
| replenishRate: 1, burstCapacity: 5     | 1 req/s steady, burst up to 5   |
| replenishRate: 100, burstCapacity: 100 | 100 req/s, no burst             |

### Key Resolution

**Current**: IP-based

```java
return exchange -> Mono.just(
    exchange.getRequest()
           .getRemoteAddress()
           .getAddress()
           .getHostAddress()
);
```

**Alternatives**:

#### User-based

```java
return exchange -> Mono.just(
    exchange.getRequest()
           .getHeaders()
           .getFirst("X-User-ID")
);
```

#### API Key-based

```java
return exchange -> Mono.just(
    exchange.getRequest()
           .getHeaders()
           .getFirst("apiKey")
);
```

#### Combined (User + Endpoint)

```java
return exchange -> {
    String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
    String path = exchange.getRequest().getPath().toString();
    return Mono.just(userId + ":" + path);
};
```

### Redis Storage

**Key Format**: `request_rate_limiter.{key}.timestamp`

**Example**:

```
Redis Key: request_rate_limiter.192.168.1.100.1642320000
Redis Value: {"tokens": 15, "timestamp": 1642320000123}
TTL: 60 seconds
```

### Custom Rate Limit per Route

```yaml
routes:
  # Standard API: 10/sec
  - id: movies-standard
    uri: lb://movies
    predicates:
      - Path=/api/v1/movies
    filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 10
          redis-rate-limiter.burstCapacity: 20

  # Premium API: 100/sec
  - id: movies-premium
    uri: lb://movies
    predicates:
      - Path=/api/premium/v1/movies
      - Header=X-Premium-User, true
    filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 100
          redis-rate-limiter.burstCapacity: 200
```

---

## 🚀 Cài đặt và chạy

### Prerequisites

- ✅ Java 17 (JDK)
- ✅ Maven 3.6+
- ✅ Redis Server 6.x+
- ✅ Eureka Server running (Port 8761)
- ✅ Backend services (e.g., Movies Service)

### Installation Steps

#### 1. Clone Repository

```bash
cd e:\fullStack\project\02_Cinema\apigateway
```

#### 2. Install Dependencies

```bash
mvn clean install
```

#### 3. Start Redis (Windows)

```bash
# Nếu đã cài Redis
redis-server

# Hoặc dùng Docker
docker run -d -p 6379:6379 redis:latest
```

#### 4. Verify Eureka Server

```bash
# Check Eureka dashboard
http://localhost:8761
```

#### 5. Configure Environment Variables (Optional)

```bash
# Windows PowerShell
$env:API_KEY="tranminhkhue"

# Windows CMD
set API_KEY=tranminhkhue

# Linux/Mac
export API_KEY=tranminhkhue
```

#### 6. Run Application

**Method 1: Maven**

```bash
mvn spring-boot:run
```

**Method 2: Java JAR**

```bash
mvn clean package
java -jar target/apigateway-0.0.1-SNAPSHOT.jar
```

**Method 3: IDE (IntelliJ/VS Code)**

- Run `ApigatewayApplication.java` main method

### Verify Startup

**Console Output**:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.1)

INFO 12345 --- [main] c.c.a.ApigatewayApplication : Starting ApigatewayApplication
INFO 12345 --- [main] o.s.c.g.route.RouteDefinitionRouteLocator : Loaded RoutePredicateFactory [Path]
INFO 12345 --- [main] o.s.c.g.route.RouteDefinitionRouteLocator : Loaded RoutePredicateFactory [Method]
INFO 12345 --- [main] c.c.a.Filter.KeyAuthFilter : KeyAuthFilter initialized
INFO 12345 --- [main] o.s.b.w.embedded.netty.NettyWebServer : Netty started on port 8080
INFO 12345 --- [main] c.n.e.EurekaDiscoveryClient : Registering application gateway-service with eureka
INFO 12345 --- [main] c.c.a.ApigatewayApplication : Started ApigatewayApplication in 5.123 seconds
```

**Check Health**:

```bash
# Gateway actuator (if enabled)
curl http://localhost:8080/actuator/health

# Eureka dashboard
# Should see "GATEWAY-SERVICE" in registered services
```

---

## 🧪 Testing

### 1. Manual Testing với curl

#### Test 1: Missing API Key (401)

```bash
curl -X GET http://localhost:8080/api/v1/movies

# Expected Response:
# Status: 401 Unauthorized
# {
#   "timestamp": "2026-01-15T10:30:00.123Z",
#   "status": 401,
#   "error": "Unauthorized",
#   "message": "Missing authorization information",
#   "path": "/api/v1/movies"
# }
```

#### Test 2: Invalid API Key (403)

```bash
curl -X GET http://localhost:8080/api/v1/movies \
  -H "apiKey: wrong-key"

# Expected Response:
# Status: 403 Forbidden
# {
#   "timestamp": "2026-01-15T10:30:00.123Z",
#   "status": 403,
#   "error": "Forbidden",
#   "message": "Invalid Api Key",
#   "path": "/api/v1/movies"
# }
```

#### Test 3: Valid API Key (200)

```bash
curl -X GET http://localhost:8080/api/v1/movies \
  -H "apiKey: tranminhkhue"

# Expected Response:
# Status: 200 OK
# [
#   {
#     "id": 1,
#     "title": "Inception",
#     "genre": "Sci-Fi"
#   }
# ]
```

#### Test 4: Rate Limiting (429)

```bash
# Send 25 requests rapidly (exceeds burst capacity of 20)
for i in {1..25}; do
  curl -X GET http://localhost:8080/api/v1/movies \
    -H "apiKey: tranminhkhue" &
done
wait

# Expected: First 20 succeed, remaining 5 return 429
# Status: 429 Too Many Requests
```

### 2. Postman Collection

**Import Collection**:

```json
{
  "info": {
    "name": "Cinema API Gateway Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Get Movies - Valid Key",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "apiKey",
            "value": "{{api_key}}",
            "type": "text"
          }
        ],
        "url": {
          "raw": "{{gateway_url}}/api/v1/movies",
          "host": ["{{gateway_url}}"],
          "path": ["api", "v1", "movies"]
        }
      }
    },
    {
      "name": "Get Movies - No Key",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{gateway_url}}/api/v1/movies",
          "host": ["{{gateway_url}}"],
          "path": ["api", "v1", "movies"]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "gateway_url",
      "value": "http://localhost:8080"
    },
    {
      "key": "api_key",
      "value": "tranminhkhue"
    }
  ]
}
```

### 3. Automated Tests

**JUnit Test Example** (`ApigatewayApplicationTests.java`):

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApigatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testMissingApiKey() {
        webTestClient.get()
            .uri("/api/v1/movies")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.message").isEqualTo("Missing authorization information");
    }

    @Test
    void testInvalidApiKey() {
        webTestClient.get()
            .uri("/api/v1/movies")
            .header("apiKey", "invalid-key")
            .exchange()
            .expectStatus().isForbidden()
            .expectBody()
            .jsonPath("$.message").isEqualTo("Invalid Api Key");
    }

    @Test
    void testValidApiKey() {
        webTestClient.get()
            .uri("/api/v1/movies")
            .header("apiKey", "tranminhkhue")
            .exchange()
            .expectStatus().isOk();
    }
}
```

**Run Tests**:

```bash
mvn test
```

### 4. Load Testing với Apache Bench

```bash
# 1000 requests, 10 concurrent
ab -n 1000 -c 10 \
   -H "apiKey: tranminhkhue" \
   http://localhost:8080/api/v1/movies

# Expected output showing rate limiting effects
```

---

## 🔍 Troubleshooting

### Common Issues

#### ❌ Issue 1: "Unable to find GatewayFilterFactory with name KeyAuth"

**Error**:

```
Caused by: java.lang.IllegalArgumentException: Unable to find GatewayFilterFactory with name KeyAuth
```

**Cause**: Spring không tìm thấy filter bean

**Solutions**:

**Option 1**: Sử dụng tên class đầy đủ

```yaml
filters:
  - name: KeyAuthFilter # ✅ Full class name
```

**Option 2**: Đổi tên class

```java
// Đổi từ KeyAuthFilter thành:
public class KeyAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<...> {
    // ...
}

// Sau đó có thể dùng:
filters:
  - KeyAuth  # ✅ Short name
```

---

#### ❌ Issue 2: Filter không được trigger

**Symptoms**: Requests không qua authentication

**Checklist**:

1. **Class có `@Component`?**

```java
@Component  // ✅ Must have
public class KeyAuthFilter extends AbstractGatewayFilterFactory<...>
```

2. **Filter có trong route config?**

```yaml
filters:
  - name: KeyAuthFilter # ✅ Must be declared
```

3. **Path có match predicate?**

```yaml
predicates:
  - Path=/api/v1/**
# Test: curl http://localhost:8080/api/v1/movies ✅
# Test: curl http://localhost:8080/health ❌ (không match)
```

4. **Debug logging**:

```yaml
logging:
  level:
    com.cinema.apigateway.Filter: TRACE
```

---

#### ❌ Issue 3: Redis Connection Failed

**Error**:

```
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**Solutions**:

1. **Check Redis running**:

```bash
# Windows
redis-cli ping
# Response: PONG

# Check port
netstat -an | findstr :6379
```

2. **Start Redis**:

```bash
redis-server

# Or Docker
docker run -d -p 6379:6379 redis:latest
```

3. **Check config**:

```yaml
spring:
  data:
    redis:
      host: localhost # ✅ Correct
      port: 6379 # ✅ Correct
```

---

#### ❌ Issue 4: Eureka Connection Failed

**Error**:

```
com.netflix.discovery.shared.transport.TransportException: Cannot execute request on any known server
```

**Solutions**:

1. **Start Eureka Server**:

```bash
cd ../discoveryserver
mvn spring-boot:run
```

2. **Verify Eureka URL**:

```bash
curl http://localhost:8761
# Should return Eureka dashboard HTML
```

3. **Check config**:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka # ✅ Must be accessible
```

4. **Disable Eureka temporarily** (for testing):

```yaml
eureka:
  client:
    enabled: false # ⚠️ Only for testing

# Change route to direct URL:
routes:
  - id: movies
    uri: http://localhost:8081 # Direct URL
```

---

#### ❌ Issue 5: Rate Limit Not Working

**Symptoms**: Không nhận 429 dù vượt limit

**Checklist**:

1. **Redis có data?**

```bash
redis-cli
> KEYS request_rate_limiter.*
> GET request_rate_limiter.192.168.1.100.1234567890
```

2. **KeyResolver defined?**

```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> Mono.just(
        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
    );
}
```

3. **Filter order correct?**

```yaml
filters:
  - name: KeyAuthFilter # ✅ Auth first
  - name: RequestRateLimiter # ✅ Rate limit second
```

4. **Test with actual IP**:

```bash
# Nếu test localhost, mọi request có cùng IP
# Simulate different IPs:
curl -H "X-Forwarded-For: 192.168.1.1" ...
curl -H "X-Forwarded-For: 192.168.1.2" ...
```

---

#### ❌ Issue 6: 503 Service Unavailable

**Error**: Gateway trả về 503

**Causes & Solutions**:

**Cause 1**: Backend service not running

```bash
# Check services in Eureka dashboard
http://localhost:8761

# Start backend service
cd ../movies
mvn spring-boot:run
```

**Cause 2**: Service registered nhưng not ready

```bash
# Wait for health check (30 seconds)
# Or check Eureka status: UP / DOWN / STARTING
```

**Cause 3**: Network issue

```bash
# Test direct connection
curl http://localhost:8081/api/v1/movies
```

---

### Debug Logging

**Enable full debug**:

```yaml
logging:
  level:
    root: INFO
    org.springframework.cloud.gateway: TRACE
    org.springframework.web.reactive: DEBUG
    com.cinema.apigateway: TRACE
    io.lettuce: DEBUG # Redis client
```

**Log Output Example**:

```
TRACE o.s.c.g.handler.RoutePredicateHandlerMapping : Route matched: movies
DEBUG o.s.c.g.handler.FilteringWebHandler : Sorted gatewayFilterFactories:
  [0] KeyAuthFilter
  [1] RequestRateLimiter
TRACE c.c.a.Filter.KeyAuthFilter : Checking apiKey header
TRACE c.c.a.Filter.KeyAuthFilter : API Key validation successful
DEBUG i.l.core.protocol.DefaultEndpoint : [channel=0x123, /localhost:6379] write() operation succeeded
```

---

## 🎓 Best Practices

### 1. Security

```yaml
# ✅ Use environment variables
apiKey: ${API_KEY:default-dev-key}

# ❌ Don't hardcode secrets
apiKey: tranminhkhue
```

```bash
# Production deployment
export API_KEY=$(openssl rand -hex 32)
java -jar apigateway.jar
```

### 2. Rate Limiting Strategies

```yaml
# Standard users: 10/sec
- name: RequestRateLimiter
  args:
    redis-rate-limiter.replenishRate: 10
    redis-rate-limiter.burstCapacity: 20

# Premium users: 100/sec (separate route)
- name: RequestRateLimiter
  args:
    redis-rate-limiter.replenishRate: 100
    redis-rate-limiter.burstCapacity: 200
```

### 3. Error Handling

```java
// Consistent error format
private Mono<Void> handleException(ServerWebExchange exchange,
                                   String message,
                                   HttpStatus status) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(ZonedDateTime.now())
        .status(status.value())
        .error(status.getReasonPhrase())
        .message(message)
        .path(exchange.getRequest().getURI().getPath())
        .build();

    return response.writeWith(
        Mono.just(response.bufferFactory().wrap(
            objectMapper.writeValueAsBytes(error)
        ))
    );
}
```

### 4. Monitoring

```yaml
# Enable actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,gateway
  endpoint:
    gateway:
      enabled: true
# Access:
# http://localhost:8080/actuator/gateway/routes
# http://localhost:8080/actuator/metrics/gateway.requests
```

### 5. CORS Configuration

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        add-to-simple-url-handler-mapping: true
        corsConfigurations:
          "[/**]":
            allowedOrigins:
              - "${ALLOWED_ORIGIN:http://localhost:3000}"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
```

### 6. Circuit Breaker Pattern

```xml
<!-- Add Resilience4j dependency -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

```yaml
filters:
  - name: CircuitBreaker
    args:
      name: moviesCircuitBreaker
      fallbackUri: forward:/fallback/movies
```

```java
@RestController
public class FallbackController {

    @GetMapping("/fallback/movies")
    public Mono<ResponseEntity<String>> moviesFallback() {
        return Mono.just(ResponseEntity.ok()
            .body("{\"message\": \"Movies service is temporarily unavailable\"}"));
    }
}
```

### 7. Request/Response Logging

```java
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        log.info("[{}] Request: {} {}",
            requestId,
            exchange.getRequest().getMethod(),
            exchange.getRequest().getURI());

        return chain.filter(exchange)
            .doFinally(signalType -> {
                long duration = System.currentTimeMillis() - startTime;
                log.info("[{}] Response: {} in {}ms",
                    requestId,
                    exchange.getResponse().getStatusCode(),
                    duration);
            });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;  // Execute last
    }
}
```

---

## 📊 Performance Tuning

### 1. Netty Configuration

```yaml
server:
  port: 8080
  netty:
    connection-timeout: 30s
    idle-timeout: 60s
```

### 2. Thread Pool Tuning

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 500
          max-idle-time: 30s
```

### 3. Redis Connection Pool

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 10
          max-idle: 8
          min-idle: 2
```

---

## 📚 References

- [Spring Cloud Gateway Documentation](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Eureka Client Configuration](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/#service-discovery-eureka-clients)
- [Redis Rate Limiting](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-redis-ratelimiter)
- [Project Reactor](https://projectreactor.io/docs/core/release/reference/)

---

## 🤝 Contributing

### Adding a New Route

1. Update `application.yml`:

```yaml
routes:
  - id: new-service
    uri: lb://new-service
    predicates:
      - Path=/api/v1/new/**
    filters:
      - name: KeyAuthFilter
```

2. Ensure backend service registered in Eureka

3. Test the route

### Adding a New Filter

1. Create filter class:

```java
@Component
public class MyCustomFilter extends AbstractGatewayFilterFactory<MyCustomFilter.Config> {
    // Implementation
}
```

2. Configure in route:

```yaml
filters:
  - name: MyCustomFilter
    args:
      param1: value1
```

---

## 📝 Version History

| Version | Date       | Changes                        |
| ------- | ---------- | ------------------------------ |
| 1.0.0   | 2026-01-15 | Initial release                |
|         |            | - KeyAuthFilter implementation |
|         |            | - Rate limiting with Redis     |
|         |            | - Eureka integration           |
|         |            | - Movies service routing       |

---

## 📞 Support

- **Issues**: Create GitHub issue
- **Questions**: Contact development team
- **Documentation**: This README

---

**Author**: Cinema Microservices Team  
**Last Updated**: January 15, 2026  
**Version**: 1.0.0
