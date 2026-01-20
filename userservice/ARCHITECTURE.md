# User Service - Kiến trúc và Luồng hoạt động Chi tiết

## 📐 Tổng quan Kiến trúc

User Service được thiết kế theo **Layered Architecture** với sự tích hợp của **Keycloak Identity Provider** để xử lý authentication và authorization.

```
┌────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                             │
│  (Web Browser, Mobile App, Other Microservices)                │
└────────────┬───────────────────────────────────────────────────┘
             │
             │ HTTP/REST
             ▼
┌────────────────────────────────────────────────────────────────┐
│                     API GATEWAY                                 │
│  - Routing                                                      │
│  - Load Balancing                                               │
│  - Token Validation                                             │
└────────────┬───────────────────────────────────────────────────┘
             │
             │ HTTP/REST
             ▼
┌────────────────────────────────────────────────────────────────┐
│                     USER SERVICE                                │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │              CONTROLLER LAYER                             │ │
│  │  ┌──────────────────┐    ┌──────────────────┐           │ │
│  │  │ UserController   │    │ PublicController │           │ │
│  │  │ (Protected)      │    │ (Public)         │           │ │
│  │  └────────┬─────────┘    └────────┬─────────┘           │ │
│  └───────────┼──────────────────────┼─────────────────────┘ │
│              │                       │                        │
│  ┌───────────▼──────────────────────▼─────────────────────┐ │
│  │              SERVICE LAYER                              │ │
│  │  ┌────────────────────────────────────────┐            │ │
│  │  │     UserServiceImpl                    │            │ │
│  │  │  - Business Logic                       │            │ │
│  │  │  - Keycloak Integration                 │            │ │
│  │  │  - Data Transformation                  │            │ │
│  │  └──────┬─────────────────────┬───────────┘            │ │
│  └─────────┼─────────────────────┼────────────────────────┘ │
│            │                     │                           │
│  ┌─────────▼──────────┐   ┌─────▼────────────────────────┐ │
│  │  REPOSITORY LAYER  │   │   FEIGN CLIENT LAYER         │ │
│  │  ┌──────────────┐  │   │  ┌────────────────────────┐ │ │
│  │  │UserRepository│  │   │  │   IdentityClient       │ │ │
│  │  │   (JPA)      │  │   │  │  (Keycloak REST API)   │ │ │
│  │  └──────┬───────┘  │   │  └───────┬────────────────┘ │ │
│  └─────────┼──────────┘   └──────────┼──────────────────┘ │
└────────────┼─────────────────────────┼────────────────────┘
             │                         │
    ┌────────▼────────┐       ┌────────▼──────────┐
    │   PostgreSQL    │       │     Keycloak      │
    │   Database      │       │ Identity Provider │
    │                 │       │                   │
    │ - User Info     │       │ - Authentication  │
    │ - Local Data    │       │ - Authorization   │
    └─────────────────┘       │ - User Credentials│
                              │ - JWT Tokens      │
                              └───────────────────┘
```

---

## 🏗️ Chi tiết các Layer

### 1. Controller Layer

#### 1.1 UserController (Protected Endpoints)

**Mục đích:** Xử lý các HTTP requests yêu cầu authentication

**Endpoints:**

- `POST /api/v1/users` - Tạo user mới
- `GET /api/v1/users` - Lấy danh sách users
- `GET /api/v1/users/{id}` - Lấy chi tiết user
- `PUT /api/v1/users/{id}` - Cập nhật user
- `DELETE /api/v1/users/{id}` - Xóa user

**Security:**

```java
// Tất cả requests phải có JWT token
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Responsibilities:**

- Validate request body
- Call service layer
- Transform response to HTTP format
- Handle exceptions

#### 1.2 PublicController (Public Endpoints)

**Mục đích:** Xử lý các requests không cần authentication

**Endpoints:**

- `POST /api/v1/public/login` - Đăng nhập

**Security:** Không yêu cầu authentication

---

### 2. Service Layer

#### UserServiceImpl

**Mục đích:** Xử lý business logic và orchestration

**Core Methods:**

##### 2.1 createUser()

```java
Luồng:
1. exchangeClientToken() → Lấy admin token từ Keycloak
2. createUser() → Tạo user trong Keycloak với admin token
3. extractUserId() → Parse userId từ Location header
4. save() → Lưu user info vào local DB
5. toDTO() → Convert entity sang DTO
```

##### 2.2 login()

```java
Luồng:
1. exchangeUserToken() → Gửi username/password đến Keycloak
2. Keycloak validate credentials
3. Return JWT tokens (access, refresh, id tokens)
```

##### 2.3 CRUD Operations

- `getAllUsers()`: Query từ local DB
- `getUserById()`: Query từ local DB
- `updateUser()`: Update local DB only
- `deleteUser()`: Delete từ local DB only

**Lưu ý quan trọng:**

- Update/Delete chỉ ảnh hưởng đến local DB
- Để update/delete trong Keycloak, cần gọi Admin API riêng

---

### 3. Repository Layer

#### 3.1 UserRepository (JPA)

**Mục đích:** Thao tác với PostgreSQL database

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository cung cấp sẵn:
    // - findAll()
    // - findById()
    // - save()
    // - deleteById()
}
```

**Entity Mapping:**

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255),        -- UUID từ Keycloak
    email VARCHAR(255) UNIQUE,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    dob DATE,
    name VARCHAR(255)
);
```

#### 3.2 IdentityClient (Feign)

**Mục đích:** Gọi Keycloak REST APIs

**Methods:**

##### exchangeClientToken()

```http
POST /realms/microservices/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
&client_id=microservices-client
&client_secret=your-secret
&scope=openid

Response:
{
  "access_token": "eyJhbG...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

##### createUser()

```http
POST /admin/realms/microservices/users
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true,
  "emailVerified": false,
  "credentials": [
    {
      "type": "password",
      "value": "password123",
      "temporary": false
    }
  ]
}

Response:
HTTP 201 Created
Location: http://keycloak/admin/realms/microservices/users/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

##### exchangeUserToken()

```http
POST /realms/microservices/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password
&client_id=microservices-client
&client_secret=your-secret
&scope=openid
&username=johndoe
&password=password123

Response:
{
  "access_token": "eyJhbG...",
  "refresh_token": "eyJhbG...",
  "id_token": "eyJhbG...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "token_type": "Bearer",
  "scope": "openid profile email"
}
```

---

## 🔐 OAuth2 Flows chi tiết

### Flow 1: Client Credentials Grant (Service-to-Service)

```
┌──────────────┐                                    ┌──────────┐
│ User Service │                                    │ Keycloak │
└──────┬───────┘                                    └────┬─────┘
       │                                                 │
       │ 1. POST /token                                  │
       │    grant_type=client_credentials                │
       │    client_id=microservices-client               │
       │    client_secret=secret                         │
       ├────────────────────────────────────────────────▶│
       │                                                 │
       │                    2. Validate Client           │
       │                       Credentials               │
       │                                                 │
       │ 3. Return Access Token                          │
       │    {                                            │
       │      "access_token": "eyJhbG...",               │
       │      "expires_in": 300,                         │
       │      "token_type": "Bearer"                     │
       │    }                                            │
       │◀────────────────────────────────────────────────┤
       │                                                 │
       │ 4. POST /admin/realms/.../users                 │
       │    Authorization: Bearer {access_token}         │
       │    Body: {...user info...}                      │
       ├────────────────────────────────────────────────▶│
       │                                                 │
       │                    5. Create User               │
       │                                                 │
       │ 6. HTTP 201 Created                             │
       │    Location: .../users/{userId}                 │
       │◀────────────────────────────────────────────────┤
       │                                                 │
```

**Đặc điểm:**

- Không cần user credentials
- Token có quyền admin để gọi Admin APIs
- Expire nhanh (5-15 phút)
- Dùng cho server-to-server communication

---

### Flow 2: Password Grant (User Authentication)

```
┌──────┐      ┌──────────────┐                    ┌──────────┐
│Client│      │ User Service │                    │ Keycloak │
└──┬───┘      └──────┬───────┘                    └────┬─────┘
   │                 │                                  │
   │ 1. POST /login  │                                  │
   │    {username, password}                            │
   ├────────────────▶│                                  │
   │                 │                                  │
   │                 │ 2. POST /token                   │
   │                 │    grant_type=password           │
   │                 │    username=johndoe              │
   │                 │    password=password123          │
   │                 ├─────────────────────────────────▶│
   │                 │                                  │
   │                 │          3. Validate User        │
   │                 │             Credentials          │
   │                 │          (check password hash)   │
   │                 │                                  │
   │                 │ 4. Return JWT Tokens             │
   │                 │    {                             │
   │                 │      "access_token": "eyJ...",   │
   │                 │      "refresh_token": "eyJ...",  │
   │                 │      "id_token": "eyJ...",       │
   │                 │      "expires_in": 300           │
   │                 │    }                             │
   │                 │◀─────────────────────────────────┤
   │                 │                                  │
   │ 5. Return Tokens│                                  │
   │◀────────────────┤                                  │
   │                 │                                  │
   │ 6. GET /users   │                                  │
   │    Authorization: Bearer {access_token}            │
   ├────────────────▶│                                  │
   │                 │                                  │
   │                 │          7. Validate Token       │
   │                 │             (check signature,    │
   │                 │              expiration)         │
   │                 │                                  │
   │ 8. Return Data  │                                  │
   │◀────────────────┤                                  │
   │                 │                                  │
```

**Đặc điểm:**

- Cần user credentials (username/password)
- Trả về 3 loại tokens: access, refresh, id
- Access token expire nhanh (5-15 phút)
- Refresh token expire chậm (30 phút - nhiều giờ)
- Client lưu tokens và gửi kèm trong mọi request

---

## 🎯 Các Use Cases chi tiết

### Use Case 1: Đăng ký User mới

**Actors:** Admin hoặc User tự đăng ký

**Preconditions:**

- Client có access token (nếu là protected endpoint)
- Username và email chưa tồn tại

**Flow:**

```
1. Client gửi request:
   POST /api/v1/users
   {
     "email": "john@example.com",
     "username": "johndoe",
     "firstName": "John",
     "lastName": "Doe",
     "password": "password123",
     "dob": "1990-01-15",
     "name": "John Doe"
   }

2. UserController nhận request
   - Validate request body
   - Gọi userService.createUser(dto)

3. UserServiceImpl.createUser()
   a. Lấy client token:
      - Call identityClient.exchangeClientToken()
      - Nhận admin access token

   b. Tạo user trong Keycloak:
      - Build UserCreationParam với credentials
      - Call identityClient.createUser(param, token)
      - Keycloak hash password và lưu
      - Keycloak trả về Location header

   c. Extract userId:
      - Parse Location: .../users/{userId}
      - Lấy UUID cuối cùng

   d. Lưu vào local DB:
      - Create User entity với userId từ Keycloak
      - Save vào PostgreSQL
      - Nhận user với ID đã generate

   e. Return DTO:
      - Convert User entity → UserResponseDTO
      - Không include password

4. UserController trả response:
   HTTP 200 OK
   {
     "id": 1,
     "userId": "f47ac10b-...",
     "email": "john@example.com",
     "username": "johndoe",
     "firstName": "John",
     "lastName": "Doe",
     "dob": "1990-01-15",
     "name": "John Doe"
   }
```

**Postconditions:**

- User tồn tại trong Keycloak (với password hash)
- User tồn tại trong local DB (không có password)
- userId mapping giữa 2 systems

---

### Use Case 2: Đăng nhập

**Actors:** User

**Preconditions:**

- User đã đăng ký
- Username và password đúng

**Flow:**

```
1. Client gửi request:
   POST /api/v1/public/login
   {
     "username": "johndoe",
     "password": "password123"
   }

2. PublicController nhận request
   - Gọi userService.login(dto)

3. UserServiceImpl.login()
   a. Exchange user token:
      - Build UserTokenExchangeParam
      - Call identityClient.exchangeUserToken()

   b. Keycloak validate:
      - Tìm user theo username
      - Compare password hash
      - Generate JWT tokens nếu đúng

   c. Return tokens:
      - access_token: JWT chứa user info, roles
      - refresh_token: Token để renew access_token
      - id_token: OpenID Connect token

4. PublicController trả response:
   HTTP 200 OK
   {
     "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     "expires_in": "300",
     "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "refresh_expires_in": "1800",
     "token_type": "Bearer",
     "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     "scope": "openid profile email"
   }

5. Client lưu tokens:
   - Store access_token in memory/localStorage
   - Store refresh_token securely
   - Gửi access_token trong mọi request tiếp theo
```

**Postconditions:**

- Client có access_token để gọi protected APIs
- Client có refresh_token để renew khi hết hạn

---

### Use Case 3: Gọi Protected API

**Actors:** Authenticated User

**Preconditions:**

- User đã đăng nhập
- Có valid access_token

**Flow:**

```
1. Client gửi request:
   GET /api/v1/users/1
   Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

2. API Gateway (hoặc Spring Security):
   a. Extract Bearer token từ header
   b. Validate token với Keycloak:
      - Check signature (public key)
      - Check expiration time
      - Check issuer (Keycloak URL)
   c. Nếu valid, forward request đến User Service
   d. Nếu invalid/expired, return 401 Unauthorized

3. UserController nhận request:
   - Security context có user info từ token
   - Gọi userService.getUserById(1)

4. UserServiceImpl.getUserById():
   - Query từ local DB
   - Convert sang DTO
   - Return

5. UserController trả response:
   HTTP 200 OK
   {
     "id": 1,
     "userId": "f47ac10b-...",
     "email": "john@example.com",
     ...
   }
```

**Error Case - Token Expired:**

```
1. Client gửi request với expired token

2. API Gateway validate token:
   - Token đã hết hạn (expires_in = 300s)
   - Return 401 Unauthorized

3. Client handle:
   a. Use refresh_token để lấy access_token mới:
      POST /realms/.../token
      grant_type=refresh_token
      refresh_token={refresh_token}

   b. Nhận access_token mới

   c. Retry original request với token mới
```

---

## 📊 Data Flow

### Cấu trúc dữ liệu User

#### Trong Keycloak:

```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true,
  "emailVerified": false,
  "credentials": [
    {
      "type": "password",
      "value": "$2a$10$N9qo8uLO...", // Hashed password
      "temporary": false
    }
  ],
  "realmRoles": ["user"],
  "clientRoles": {
    "account": ["manage-account", "view-profile"]
  }
}
```

#### Trong Local Database:

```sql
SELECT * FROM users WHERE id = 1;

 id |              user_id               |      email        | username | first_name | last_name |    dob     |   name
----+------------------------------------+-------------------+----------+------------+-----------+------------+-----------
  1 | f47ac10b-58cc-4372-a567-0e02b2c3d479 | john@example.com | johndoe  | John       | Doe       | 1990-01-15 | John Doe
```

**Quan hệ:**

- `users.user_id` = `keycloak_user.id` (UUID)
- Password CHỈ lưu trong Keycloak (hashed)
- Local DB chỉ lưu thông tin profile

---

## 🔒 Security Considerations

### 1. Password Security

- **Plain password**: Chỉ tồn tại trong request (HTTPS encrypted)
- **Keycloak**: Hash với bcrypt/pbkdf2
- **Never**: Lưu plain password trong logs, DB, cache

### 2. Token Security

- **Access Token**: Short-lived (5-15 min), chứa claims
- **Refresh Token**: Long-lived (30 min - hours), opaque
- **Storage**: Access token in memory, refresh token in httpOnly cookie

### 3. Communication Security

- **HTTPS**: Tất cả communication phải qua TLS
- **Keycloak**: SSL/TLS cho token endpoints
- **Database**: SSL connection cho PostgreSQL

### 4. Authorization

```java
// JWT Access Token structure:
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "f47ac10b-...",      // Subject (user ID)
    "name": "John Doe",
    "preferred_username": "johndoe",
    "email": "john@example.com",
    "realm_access": {
      "roles": ["user"]
    },
    "scope": "openid profile email",
    "iat": 1609459200,          // Issued at
    "exp": 1609459500           // Expiration
  },
  "signature": "..."
}
```

---

## 🚀 Scalability & Performance

### Horizontal Scaling

```
┌─────────────┐
│ Load        │
│ Balancer    │
└──────┬──────┘
       │
       ├────────────┬────────────┐
       │            │            │
  ┌────▼────┐  ┌────▼────┐  ┌────▼────┐
  │ User    │  │ User    │  │ User    │
  │ Service │  │ Service │  │ Service │
  │ Pod 1   │  │ Pod 2   │  │ Pod 3   │
  └────┬────┘  └────┬────┘  └────┬────┘
       │            │            │
       └────────────┴────────────┘
                    │
       ┌────────────▼────────────┐
       │   PostgreSQL            │
       │   (Master + Replicas)   │
       └─────────────────────────┘
```

### Caching Strategy

```java
// Cache user info sau khi query từ DB
@Cacheable(value = "users", key = "#id")
public UserResponseDTO getUserById(Long id) {
    // Cache miss → Query DB
    // Cache hit → Return từ cache
}

// Invalidate cache khi update
@CacheEvict(value = "users", key = "#id")
public UserResponseDTO updateUser(Long id, ...) {
    // Clear cache entry
}
```

### Database Optimization

```sql
-- Indexes cho performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_user_id ON users(user_id);

-- Connection pooling in application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

---

## 📈 Monitoring & Observability

### Metrics to Track

- Request latency (p50, p95, p99)
- Error rate (4xx, 5xx)
- Token exchange success/failure rate
- Database connection pool usage
- Keycloak API response time

### Logging Strategy

```java
@Slf4j
public class UserServiceImpl {
    public UserResponseDTO createUser(...) {
        log.info("Creating user with email: {}", dto.getEmail());
        try {
            // ... logic
            log.info("User created successfully with userId: {}", userId);
        } catch (Exception e) {
            log.error("Failed to create user", e);
            throw e;
        }
    }
}
```

---

## 🎓 Best Practices được áp dụng

1. **Separation of Concerns**: Mỗi layer có trách nhiệm rõ ràng
2. **DRY Principle**: Không duplicate code
3. **Single Responsibility**: Mỗi class/method làm 1 việc
4. **Dependency Injection**: Loosely coupled components
5. **DTO Pattern**: Không expose entities ra ngoài
6. **Builder Pattern**: Dễ dàng tạo objects phức tạp
7. **Exception Handling**: Centralized error handling
8. **Logging**: Structured logging cho debugging
9. **Security**: Password hashing, JWT tokens, HTTPS

---

Để hiểu sâu hơn, đọc thêm:

- [README.md](README.md) - Hướng dẫn cài đặt và sử dụng
- Source code với chi tiết JavaDoc comments trong từng file
