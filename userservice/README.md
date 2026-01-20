# User Service - Microservice Quản lý Người dùng

## 📋 Mục lục

1. [Tổng quan](#tổng-quan)
2. [Kiến trúc](#kiến-trúc)
3. [Luồng hoạt động](#luồng-hoạt-động)
4. [API Endpoints](#api-endpoints)
5. [Cấu hình](#cấu-hình)
6. [Hướng dẫn sử dụng](#hướng-dẫn-sử-dụng)

---

## 🎯 Tổng quan

**User Service** là microservice chịu trách nhiệm quản lý người dùng trong hệ thống, tích hợp với **Keycloak** (Identity Provider) để xử lý xác thực và phân quyền theo chuẩn OAuth2/OpenID Connect.

### Chức năng chính:

- ✅ Đăng ký người dùng mới
- ✅ Đăng nhập với username/password
- ✅ Quản lý thông tin người dùng (CRUD)
- ✅ Tích hợp Keycloak cho Identity Management
- ✅ Phát hành và quản lý JWT tokens

### Công nghệ sử dụng:

- **Spring Boot 3.x** - Application framework
- **Spring Cloud** - Service discovery (Eureka Client)
- **OpenFeign** - HTTP client cho Keycloak integration
- **Spring Data JPA** - Database persistence
- **PostgreSQL/MySQL** - Relational database
- **Keycloak** - Identity and Access Management
- **Lombok** - Code generation
- **Maven** - Build tool

---

## 🏗️ Kiến trúc

```
┌─────────────────────────────────────────────────────────────┐
│                     USER SERVICE                             │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────┐         ┌──────────────────┐          │
│  │   Controllers   │────────▶│    Services      │          │
│  │                 │         │                  │          │
│  │ - UserController│         │ - UserServiceImpl│          │
│  │ - PublicCtrl    │         │                  │          │
│  └─────────────────┘         └────────┬─────────┘          │
│                                        │                     │
│                           ┌────────────┴────────────┐       │
│                           │                         │       │
│                  ┌────────▼─────────┐    ┌─────────▼─────┐ │
│                  │  UserRepository  │    │ IdentityClient│ │
│                  │     (JPA)        │    │   (Feign)     │ │
│                  └────────┬─────────┘    └─────────┬─────┘ │
│                           │                        │        │
└───────────────────────────┼────────────────────────┼────────┘
                            │                        │
                    ┌───────▼────────┐      ┌────────▼────────┐
                    │   PostgreSQL   │      │    Keycloak     │
                    │   Database     │      │  Identity Server│
                    └────────────────┘      └─────────────────┘
```

### Layers:

#### 1. **Controller Layer** (API Endpoints)

- `UserController`: Protected endpoints - yêu cầu authentication
- `PublicController`: Public endpoints - không yêu cầu authentication (login)

#### 2. **Service Layer** (Business Logic)

- `IUserService`: Interface định nghĩa business operations
- `UserServiceImpl`: Implementation xử lý logic nghiệp vụ

#### 3. **Repository Layer** (Data Access)

- `UserRepository`: JPA repository cho local database
- `IdentityClient`: Feign client gọi Keycloak REST APIs

#### 4. **Entity Layer** (Domain Model)

- `User`: JPA entity mapping với bảng users

#### 5. **DTO Layer** (Data Transfer Objects)

- Request DTOs: `CreateUserRequestDTO`, `LoginRequestDto`
- Response DTOs: `UserResponseDTO`, `TokenExchangeResponse`
- Identity DTOs: Các DTO cho Keycloak integration

---

## 🔄 Luồng hoạt động

### 1️⃣ **Luồng Đăng ký User mới**

```
┌─────────┐         ┌─────────────┐         ┌──────────────┐         ┌──────────┐         ┌──────────┐
│ Client  │         │UserController│        │UserServiceImpl│        │ Keycloak │         │   DB     │
└────┬────┘         └──────┬──────┘         └──────┬───────┘         └────┬─────┘         └────┬─────┘
     │                     │                       │                      │                     │
     │ 1. POST /users      │                       │                      │                     │
     │ (CreateUserDTO)     │                       │                      │                     │
     ├────────────────────▶│                       │                      │                     │
     │                     │                       │                      │                     │
     │                     │ 2. createUser(dto)    │                      │                     │
     │                     ├──────────────────────▶│                      │                     │
     │                     │                       │                      │                     │
     │                     │                       │ 3. Get Client Token  │                     │
     │                     │                       │ (client_credentials) │                     │
     │                     │                       ├─────────────────────▶│                     │
     │                     │                       │                      │                     │
     │                     │                       │ 4. Return Token      │                     │
     │                     │                       │◀─────────────────────┤                     │
     │                     │                       │                      │                     │
     │                     │                       │ 5. Create User       │                     │
     │                     │                       │ (with Bearer token)  │                     │
     │                     │                       ├─────────────────────▶│                     │
     │                     │                       │                      │                     │
     │                     │                       │ 6. Return Location   │                     │
     │                     │                       │    header (userId)   │                     │
     │                     │                       │◀─────────────────────┤                     │
     │                     │                       │                      │                     │
     │                     │                       │ 7. Save User to DB                         │
     │                     │                       ├───────────────────────────────────────────▶│
     │                     │                       │                      │                     │
     │                     │                       │ 8. Return saved user                       │
     │                     │                       │◀───────────────────────────────────────────┤
     │                     │                       │                      │                     │
     │                     │ 9. Return UserDTO     │                      │                     │
     │                     │◀──────────────────────┤                      │                     │
     │                     │                       │                      │                     │
     │ 10. 200 OK          │                       │                      │                     │
     │ (UserResponseDTO)   │                       │                      │                     │
     │◀────────────────────┤                       │                      │                     │
     │                     │                       │                      │                     │
```

**Chi tiết các bước:**

1. **Client gửi request đăng ký** với thông tin user (email, username, password, ...)
2. **Controller** forward request đến Service layer
3. **Service lấy Client Access Token** từ Keycloak:
   ```
   POST /realms/microservices/protocol/openid-connect/token
   Body: grant_type=client_credentials&client_id=...&client_secret=...
   ```
4. **Keycloak trả về token** cho service (server-to-server authentication)
5. **Service tạo user trong Keycloak**:
   ```
   POST /admin/realms/microservices/users
   Headers: Authorization: Bearer {token}
   Body: {username, email, password, ...}
   ```
6. **Keycloak trả về** HTTP 201 với header `Location: .../users/{userId}`
7. **Service extract userId** từ Location header và lưu user vào local DB
8. **Database trả về** user đã lưu với ID được generate
9. **Service convert** entity sang DTO và trả về controller
10. **Controller trả về** response 200 OK với UserResponseDTO

---

### 2️⃣ **Luồng Đăng nhập User**

```
┌─────────┐         ┌──────────────┐         ┌──────────────┐         ┌──────────┐
│ Client  │         │PublicController│       │UserServiceImpl│        │ Keycloak │
└────┬────┘         └──────┬───────┘         └──────┬───────┘         └────┬─────┘
     │                     │                        │                      │
     │ 1. POST /login      │                        │                      │
     │ {username, password}│                        │                      │
     ├────────────────────▶│                        │                      │
     │                     │                        │                      │
     │                     │ 2. login(dto)          │                      │
     │                     ├───────────────────────▶│                      │
     │                     │                        │                      │
     │                     │                        │ 3. Token Exchange    │
     │                     │                        │ (password grant)     │
     │                     │                        ├─────────────────────▶│
     │                     │                        │                      │
     │                     │                        │ grant_type=password  │
     │                     │                        │ username=...         │
     │                     │                        │ password=...         │
     │                     │                        │                      │
     │                     │                        │ 4. Validate & Return │
     │                     │                        │    JWT Tokens        │
     │                     │                        │◀─────────────────────┤
     │                     │                        │                      │
     │                     │ 5. Return tokens       │                      │
     │                     │◀───────────────────────┤                      │
     │                     │                        │                      │
     │ 6. 200 OK           │                        │                      │
     │ {access_token,      │                        │                      │
     │  refresh_token,     │                        │                      │
     │  id_token}          │                        │                      │
     │◀────────────────────┤                        │                      │
     │                     │                        │                      │
```

**Chi tiết các bước:**

1. **Client gửi request login** với username và password
2. **Controller** forward request đến Service
3. **Service gọi Keycloak Token Endpoint**:
   ```
   POST /realms/microservices/protocol/openid-connect/token
   Body: grant_type=password&username=...&password=...&client_id=...&client_secret=...
   ```
4. **Keycloak validate credentials** và trả về JWT tokens:
   ```json
   {
     "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     "expires_in": 300,
     "refresh_expires_in": 1800,
     "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "token_type": "Bearer",
     "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     "scope": "openid profile email"
   }
   ```
5. **Service trả về** tokens cho controller
6. **Controller trả về** response với tokens cho client

**Client sử dụng access_token** cho các request tiếp theo:

```
Authorization: Bearer {access_token}
```

---

### 3️⃣ **Luồng lấy thông tin User**

```
┌─────────┐         ┌─────────────┐         ┌──────────────┐         ┌──────────┐
│ Client  │         │UserController│        │UserServiceImpl│        │   DB     │
└────┬────┘         └──────┬──────┘         └──────┬───────┘         └────┬─────┘
     │                     │                       │                      │
     │ 1. GET /users/{id}  │                       │                      │
     │ Authorization: Bearer│                      │                      │
     ├────────────────────▶│                       │                      │
     │                     │                       │                      │
     │                     │ 2. getUserById(id)    │                      │
     │                     ├──────────────────────▶│                      │
     │                     │                       │                      │
     │                     │                       │ 3. findById(id)      │
     │                     │                       ├─────────────────────▶│
     │                     │                       │                      │
     │                     │                       │ 4. Return User       │
     │                     │                       │◀─────────────────────┤
     │                     │                       │                      │
     │                     │ 5. Return UserDTO     │                      │
     │                     │◀──────────────────────┤                      │
     │                     │                       │                      │
     │ 6. 200 OK           │                       │                      │
     │ (UserResponseDTO)   │                       │                      │
     │◀────────────────────┤                       │                      │
     │                     │                       │                      │
```

---

## 📡 API Endpoints

### **Public Endpoints** (Không cần authentication)

#### 1. Đăng nhập

```http
POST /api/v1/public/login
Content-Type: application/json

{
  "username": "johndoe",
  "password": "password123"
}
```

**Response:**

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": "300",
  "refresh_expires_in": "1800",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "scope": "openid profile email"
}
```

---

### **Protected Endpoints** (Cần authentication)

#### 2. Tạo User mới

```http
POST /api/v1/users
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe",
  "dob": "1990-01-15",
  "name": "John Doe",
  "password": "securePassword123"
}
```

**Response:**

```json
{
  "id": 1,
  "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "email": "john.doe@example.com",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe",
  "dob": "1990-01-15",
  "name": "John Doe"
}
```

#### 3. Lấy danh sách tất cả Users

```http
GET /api/v1/users
Authorization: Bearer {access_token}
```

**Response:**

```json
[
  {
    "id": 1,
    "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "email": "john.doe@example.com",
    "username": "johndoe",
    "firstName": "John",
    "lastName": "Doe",
    "dob": "1990-01-15",
    "name": "John Doe"
  },
  ...
]
```

#### 4. Lấy thông tin User theo ID

```http
GET /api/v1/users/{id}
Authorization: Bearer {access_token}
```

#### 5. Cập nhật User

```http
PUT /api/v1/users/{id}
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "email": "john.updated@example.com",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe Updated",
  "dob": "1990-01-15",
  "name": "John Doe Updated"
}
```

#### 6. Xóa User

```http
DELETE /api/v1/users/{id}
Authorization: Bearer {access_token}
```

**Response:** 204 No Content

---

## ⚙️ Cấu hình

### application.yml

```yaml
spring:
  application:
    name: user-service

  # Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver

  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

# Server Configuration
server:
  port: 8081

# Eureka Service Discovery
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

# Keycloak Identity Provider Configuration
idp:
  url: http://localhost:8180 # Keycloak server URL
  client-id: microservices-client # OAuth2 client ID
  client-secret: your-client-secret # OAuth2 client secret
```

---

## 🚀 Hướng dẫn sử dụng

### 1. Cài đặt và chạy Keycloak

```bash
# Download Keycloak
# https://www.keycloak.org/downloads

# Start Keycloak
cd keycloak-{version}/bin
./kc.sh start-dev --http-port=8180
```

**Cấu hình Keycloak:**

1. Truy cập: http://localhost:8180
2. Tạo realm: `microservices`
3. Tạo client:
   - Client ID: `microservices-client`
   - Client Protocol: `openid-connect`
   - Access Type: `confidential`
   - Valid Redirect URIs: `*`
   - Copy Client Secret từ Credentials tab

### 2. Khởi động Database

```bash
# PostgreSQL with Docker
docker run --name postgres-userdb \
  -e POSTGRES_DB=userdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

### 3. Build và chạy User Service

```bash
# Build project
mvn clean package

# Run application
mvn spring-boot:run

# Hoặc chạy JAR file
java -jar target/userservice-0.0.1-SNAPSHOT.jar
```

### 4. Test API

#### Đăng ký user mới:

```bash
curl -X POST http://localhost:8081/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "username": "testuser",
    "firstName": "Test",
    "lastName": "User",
    "dob": "1990-01-01",
    "name": "Test User",
    "password": "password123"
  }'
```

#### Đăng nhập:

```bash
curl -X POST http://localhost:8081/api/v1/public/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

#### Lấy danh sách users (với token):

```bash
curl -X GET http://localhost:8081/api/v1/users \
  -H "Authorization: Bearer {access_token}"
```

---

## 🔐 Security Flow

### OAuth2 Password Grant Flow:

```
┌────────┐                                           ┌──────────┐
│        │─(1) Login (username/password)────────────▶│          │
│        │                                            │          │
│ Client │◀(2) Access Token + Refresh Token ─────────│ Keycloak │
│        │                                            │          │
│        │─(3) API Request + Access Token ──────────▶│ Service  │
│        │                                            │          │
│        │◀(4) Protected Resource ───────────────────│          │
└────────┘                                           └──────────┘
```

### Client Credentials Grant Flow (Service-to-Service):

```
┌─────────────┐                                     ┌──────────┐
│             │─(1) Request Token                   │          │
│             │     (client_id + client_secret)────▶│          │
│ User Service│                                     │ Keycloak │
│             │◀(2) Access Token ───────────────────│          │
│             │                                     │          │
│             │─(3) Create User (with token) ──────▶│          │
└─────────────┘                                     └──────────┘
```

---

## 📊 Database Schema

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255),           -- UUID từ Keycloak
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    dob DATE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_user_id ON users(user_id);
```

---

## 🔧 Troubleshooting

### 1. Lỗi kết nối Keycloak

```
Feign.FeignException: Connection refused
```

**Giải pháp:** Kiểm tra Keycloak đã chạy và URL trong `application.yml` đúng.

### 2. Lỗi Client Credentials

```
401 Unauthorized: Invalid client credentials
```

**Giải pháp:** Kiểm tra `client-id` và `client-secret` trong config.

### 3. Lỗi Database Connection

```
org.postgresql.util.PSQLException: Connection refused
```

**Giải pháp:** Kiểm tra PostgreSQL đang chạy và thông tin kết nối đúng.

---

## 📝 Notes

- Password được hash và lưu trữ trong Keycloak, **KHÔNG** lưu trong database local
- `userId` là UUID được generate bởi Keycloak khi tạo user
- Access token có thời gian sống ngắn (mặc định 5 phút)
- Refresh token có thời gian sống dài hơn (mặc định 30 phút)
- Sử dụng refresh token để lấy access token mới khi hết hạn

---

## 🤝 Tích hợp với các service khác

User Service có thể được gọi bởi:

- **API Gateway** - Route requests và authentication
- **Borrowing Service** - Kiểm tra thông tin user khi mượn sách
- **Notification Service** - Gửi thông báo cho user
- **Employee Service** - Quản lý nhân viên (có thể extend từ User)

---

## 📚 Tài liệu tham khảo

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OAuth2 & OpenID Connect Specs](https://oauth.net/2/)
- [Spring Cloud OpenFeign](https://spring.io/projects/spring-cloud-openfeign)
- [Spring Security OAuth2](https://spring.io/projects/spring-security-oauth)
