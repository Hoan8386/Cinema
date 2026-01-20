# 📚 TÀI LIỆU COMMON SERVICE

## 🎯 COMMON SERVICE LÀ GÌ?

**Common Service** là một **shared library** (thư viện dùng chung) chứa các code, logic, và components được sử dụng lại ở nhiều microservice khác nhau trong hệ thống.

**Trong project của bạn:**

- `commonservice` là một Maven module độc lập
- Được build thành file `.jar`
- Các service khác (movies, cinema, seat, showtime) import nó như một dependency
- Mục đích: **Tránh code trùng lặp, duy trì tính nhất quán**

---

## 📦 CẤU TRÚC COMMON SERVICE

```
commonservice/
├── src/main/java/com/cinema/commonservice/
│   ├── CommonserviceApplication.java    # Main class (không cần chạy)
│   ├── advice/
│   │   └── ExceptionAdvice.java         # Xử lý exception toàn cục
│   └── model/
│       └── ErrorMessage.java             # Model cho error response
└── pom.xml                               # Maven config
```

---

## 🔧 CÁC THÀNH PHẦN TRONG COMMON SERVICE

### **1. ExceptionAdvice.java** - Xử lý Exception Toàn Cục

**Tác dụng:**

- Bắt và xử lý tất cả exception từ Controller layer
- Format error response thành format chuẩn
- Tránh phải viết try-catch ở mọi nơi

**Các handler:**

#### **a) handleValidationExceptions** - Xử lý Validation Error

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, String>> handleValiExceptions(...)
```

**Khi nào chạy:**

- Client gửi request với dữ liệu không hợp lệ
- Ví dụ: Thiếu trường bắt buộc, sai format, vượt quá độ dài

**Response trả về:**

```json
{
  "title": "Tiêu đề phim không được để trống",
  "duration": "Thời lượng phim không được để trống"
}
```

**Status:** `400 BAD REQUEST`

---

#### **b) handleCompletionException** - Xử lý Axon Framework Error

```java
@ExceptionHandler(CompletionException.class)
public ResponseEntity<ErrorMessage> handleCompletionException(...)
```

**Khi nào chạy:**

- Axon Framework throw exception từ Aggregate/CommandHandler
- Ví dụ: Không tìm thấy entity, business rule violation

**Response trả về:**

```json
{
  "code": "NOT_FOUND",
  "massage": "Movie with id 123 not found",
  "status": "NOT_FOUND"
}
```

**Status:** `404 NOT FOUND`

##### **🔍 TẠI SAO LẠI DÙNG CompletionException?**

**CompletionException là gì?**

`CompletionException` là một wrapper exception từ Java's CompletableFuture API:

- Nó **bọc (wrap)** exception thực sự bên trong
- Được throw khi một CompletableFuture hoàn thành với lỗi
- Exception thực sự nằm trong `.getCause()`

**Axon Framework và CompletionException:**

Axon Framework sử dụng **asynchronous processing** (xử lý bất đồng bộ):

```java
// Trong Controller
@PostMapping
public String createMovie(@Valid @RequestBody MovieRequestModel model) {
    CreateMovieCommand command = new CreateMovieCommand(...);
    return commandGateway.sendAndWait(command);  // ← Đây là async
}
```

**Điều gì xảy ra bên trong:**

1. **`commandGateway.sendAndWait()`**:
   - Gửi command đến Axon CommandBus
   - Command được xử lý **bất đồng bộ** (trong thread khác)
   - Trả về một `CompletableFuture<String>`

2. **Trong Aggregate:**

```java
@CommandHandler
public MovieAggregate(CreateMovieCommand command) {
    // Nếu có lỗi ở đây
    if (command.getTitle() == null) {
        throw new IllegalArgumentException("Title không được null");
        // ← Exception này sẽ bị wrap trong CompletionException
    }
}
```

3. **`sendAndWait()` chờ kết quả:**

```java
// Axon Framework internally:
CompletableFuture<String> future = commandBus.dispatch(command);
return future.join();  // ← join() sẽ throw CompletionException nếu có lỗi
```

**Tại sao phải unwrap (lấy cause)?**

```java
@ExceptionHandler(CompletionException.class)
public ResponseEntity<ErrorMessage> handleCompletionException(CompletionException ex) {
    // Unwrap để lấy exception THỰC SỰ
    Throwable cause = ex.getCause();
    String message = cause != null ? cause.getMessage() : ex.getMessage();

    return new ResponseEntity<>(new ErrorMessage("NOT_FOUND", message, HttpStatus.NOT_FOUND),
            HttpStatus.NOT_FOUND);
}
```

**Ví dụ so sánh:**

- ❌ **Không unwrap:** `CompletionException: java.lang.IllegalArgumentException: Movie not found`
  - Message dài dòng, khó hiểu cho client

- ✅ **Có unwrap:** `Movie not found`
  - Message rõ ràng, client dễ hiểu

**Flow hoàn chỉnh khi có lỗi:**

```
1. Client gửi request
   ↓
2. Controller nhận request → gọi commandGateway.sendAndWait()
   ↓
3. Axon gửi command đến CommandBus (async)
   ↓
4. MovieAggregate xử lý command
   ↓
5. Aggregate throw IllegalArgumentException("Movie not found")
   ↓
6. CompletableFuture bắt exception → wrap thành CompletionException
   ↓
7. sendAndWait().join() throw CompletionException
   ↓
8. ExceptionAdvice bắt CompletionException
   ↓
9. Unwrap để lấy IllegalArgumentException
   ↓
10. Trả về error message rõ ràng cho client
```

**Cải tiến có thể làm:**

Hiện tại code **hardcode** status là NOT_FOUND, nhưng có thể cải thiện bằng cách xác định status dựa trên loại exception thực sự:

```java
@ExceptionHandler(CompletionException.class)
public ResponseEntity<ErrorMessage> handleCompletionException(CompletionException ex) {
    Throwable cause = ex.getCause();
    String message = cause != null ? cause.getMessage() : ex.getMessage();

    // Xác định status code dựa trên loại exception thực sự
    HttpStatus status;
    String code;

    if (cause instanceof IllegalArgumentException) {
        status = HttpStatus.BAD_REQUEST;
        code = "BAD_REQUEST";
    } else if (cause instanceof EntityNotFoundException) {
        status = HttpStatus.NOT_FOUND;
        code = "NOT_FOUND";
    } else if (cause instanceof IllegalStateException) {
        status = HttpStatus.CONFLICT;
        code = "CONFLICT";
    } else {
        status = HttpStatus.INTERNAL_SERVER_ERROR;
        code = "INTERNAL_ERROR";
    }

    return new ResponseEntity<>(new ErrorMessage(code, message, status), status);
}
```

**→ CompletionException handler là BẮT BUỘC khi dùng Axon Framework với synchronous command execution (`sendAndWait()`)!**

---

#### **c) handleException** - Xử lý Mọi Exception Khác

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorMessage> handleException(...)
```

**Khi nào chạy:**

- Mọi exception không được xử lý bởi handler cụ thể
- Ví dụ: NullPointerException, IllegalArgumentException, RuntimeException tự định nghĩa

**Response trả về:**

```json
{
  "code": "9999",
  "massage": "Internal server error message",
  "status": "INTERNAL_SERVER_ERROR"
}
```

**Status:** `500 INTERNAL SERVER ERROR`

---

### **2. ErrorMessage.java** - Model Cho Error Response

**Tác dụng:**

- Định nghĩa cấu trúc chuẩn cho error response
- Đảm bảo tất cả error đều có format giống nhau

**Cấu trúc:**

```java
public class ErrorMessage {
    private String code;        // Mã lỗi (ví dụ: "9999", "NOT_FOUND")
    private String massage;     // Message mô tả lỗi
    private HttpStatus status;  // HTTP status (404, 500, etc.)
}
```

---

## 🔄 CÁCH COMMON SERVICE HOẠT ĐỘNG

### **Bước 1: Build Common Service**

```bash
cd commonservice
mvnw clean install
```

- Build thành file `.jar`
- Install vào **local Maven repository** (`~/.m2/repository/`)

### **Bước 2: Service Khác Import Common Service**

**Trong movies/pom.xml:**

```xml
<dependency>
    <groupId>com.cinema</groupId>
    <artifactId>commonservice</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### **Bước 3: Scan Components Từ Common Service**

**Trong MoviesApplication.java:**

```java
@SpringBootApplication
@ComponentScan({ "com.cinema.movies", "com.cinema.commonservice" })
public class MoviesApplication {
    // Spring sẽ scan và load ExceptionAdvice từ commonservice
}
```

### **Bước 4: Tự Động Áp Dụng**

- Spring Boot tự động phát hiện `@ControllerAdvice` từ commonservice
- Áp dụng cho tất cả Controller trong movies service
- Exception được bắt và xử lý tự động

---

## 📋 WORKFLOW XỬ LÝ VALIDATION ERROR

### **Bước 1: Client gửi request thiếu thông tin**

```http
POST /api/v1/movies
Content-Type: application/json

{
  "description": "Phim hành động",
  "duration": 120
}
```

_Thiếu trường "title" (bắt buộc)_

### **Bước 2: Request đến Controller**

- Spring Boot nhận request
- Mapping đến method `createMovie(@Valid @RequestBody MovieRequestModel model)`
- **Annotation `@Valid`** kích hoạt quá trình validation

### **Bước 3: Spring Boot thực hiện Validation**

- Spring đọc các annotation trong `MovieRequestModel`:
  - `@NotBlank` → kiểm tra field có trống không
  - `@Size` → kiểm tra độ dài
  - `@NotNull` → kiểm tra null
  - `@Min`, `@Max` → kiểm tra giá trị số
- Spring phát hiện lỗi: field `title` vi phạm `@NotBlank`

### **Bước 4: Spring throw Exception**

- Spring **KHÔNG** cho code vào trong method body
- Throw exception: `MethodArgumentNotValidException`
- Exception này chứa:
  - Danh sách field bị lỗi (ví dụ: "title")
  - Message tương ứng (ví dụ: "Tiêu đề phim không được để trống")

### **Bước 5: ExceptionAdvice bắt Exception**

- Spring tìm kiếm class có `@ControllerAdvice`
- Tìm thấy `ExceptionAdvice` trong package `com.cinema.commonservice`
- ExceptionAdvice có method `handleValiExceptions` với:
  - `@ExceptionHandler(MethodArgumentNotValidException.class)` → bắt đúng loại exception này

### **Bước 6: Xử lý và format lỗi**

```java
// ExceptionAdvice thực hiện:
1. Tạo Map<String, String> để chứa lỗi
2. Lặp qua tất cả lỗi trong exception
3. Mỗi lỗi lấy ra:
   - fieldName = "title"
   - message = "Tiêu đề phim không được để trống"
4. Cho vào Map: errors.put("title", "Tiêu đề phim không được để trống")
5. Trả về ResponseEntity với:
   - Body: Map chứa các lỗi
   - Status: 400 BAD_REQUEST
```

### **Bước 7: Response trả về Client**

```json
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "title": "Tiêu đề phim không được để trống"
}
```

---

## ⚙️ CÁC THÀNH PHẦN CẦN THIẾT

### ✅ **1. Controller phải có `@Valid`**

```java
@PostMapping
public String createMovie(@Valid @RequestBody MovieRequestModel model) {
    // ...
}
```

- Không có `@Valid` → Spring không validate
- Request sẽ vào thẳng method body với dữ liệu lỗi

### ✅ **2. Model phải có validation annotations**

```java
public class MovieRequestModel {
    @NotBlank(message = "Tiêu đề phim không được để trống")
    private String title;

    @NotNull(message = "Thời lượng phim không được để trống")
    @Min(value = 1, message = "Thời lượng phim phải lớn hơn 0")
    private Integer duration;
}
```

### ✅ **3. Class ExceptionAdvice phải có:**

- `@ControllerAdvice` → Spring tự động scan
- `@ExceptionHandler(MethodArgumentNotValidException.class)` → bắt validation error
- Logic format lỗi thành Map<String, String>

### ✅ **4. ExceptionAdvice phải được Spring scan**

- Qua `@ComponentScan` trong Application class
- Hoặc nằm trong cùng package/sub-package với Application class

### ✅ **5. Dependency commonservice phải đúng**

- GroupId phải khớp trong pom.xml
- Phải build commonservice trước → install vào local Maven repository
- Sau đó build service sử dụng (movies)

---

## 🎯 LỢI ÍCH CỦA COMMON SERVICE

### **1. Tránh Code Trùng Lặp (DRY - Don't Repeat Yourself)**

#### ❌ Không có Common Service:

```
movies/
  └── ExceptionAdvice.java      (code giống nhau)
cinema/
  └── ExceptionAdvice.java      (code giống nhau)
seat/
  └── ExceptionAdvice.java      (code giống nhau)
showtime/
  └── ExceptionAdvice.java      (code giống nhau)
```

#### ✅ Có Common Service:

```
commonservice/
  └── ExceptionAdvice.java      (code duy nhất)

movies/       → import commonservice
cinema/       → import commonservice
seat/         → import commonservice
showtime/     → import commonservice
```

### **2. Dễ Bảo Trì**

- Sửa 1 chỗ → áp dụng cho tất cả service
- Không cần update từng service một

**Ví dụ:** Muốn thêm logging cho tất cả exception:

```java
// Sửa trong commonservice/ExceptionAdvice.java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorMessage> handleException(Exception ex) {
    log.error("Error occurred: {}", ex.getMessage());  // Thêm dòng này
    return new ResponseEntity<>(...);
}
// Rebuild commonservice → rebuild các service khác → Done!
```

### **3. Tính Nhất Quán**

- Tất cả service trả về error với format giống nhau
- Client chỉ cần parse 1 format duy nhất
- Dễ debug, dễ monitor

### **4. Tái Sử Dụng**

Có thể thêm nhiều components khác vào commonservice:

- Security config
- JWT utilities
- Common DTOs
- Utility classes
- Custom annotations
- Base entities

---

## 📝 VÍ DỤ THỰC TÉ

### **Scenario: Tạo Movie Thiếu Trường Title**

#### **1. Request:**

```http
POST http://localhost:8081/api/v1/movies
Content-Type: application/json

{
  "description": "Phim hay",
  "duration": 120
}
```

#### **2. Movies Service nhận request**

```java
@PostMapping
public String createMovie(@Valid @RequestBody MovieRequestModel model) {
    // Spring validate trước khi vào đây
}
```

#### **3. Validation fail**

- Field `title` vi phạm `@NotBlank`
- Spring throw `MethodArgumentNotValidException`

#### **4. ExceptionAdvice (từ commonservice) bắt exception**

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, String>> handleValiExceptions(...) {
    Map<String, String> errors = new HashMap<>();
    errors.put("title", "Tiêu đề phim không được để trống");
    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
}
```

#### **5. Response trả về Client:**

```json
{
  "title": "Tiêu đề phim không được để trống"
}
```

**→ Không cần viết code xử lý error trong MoviesController!**

---

## 🚀 CÁC SERVICE KHÁC SỬ DỤNG TƯƠNG TỰ

Tất cả các service trong hệ thống đều có thể import và sử dụng commonservice:

```
Cinema System
├── commonservice         (Library dùng chung)
│
├── movies               → import commonservice
├── cinema               → import commonservice
├── seat                 → import commonservice
├── showtime             → import commonservice
└── booking              → import commonservice
```

**Mỗi service chỉ cần:**

1. Thêm dependency trong pom.xml
2. Thêm `@ComponentScan` để scan commonservice package
3. Rebuild project

→ Tự động có exception handling chuẩn!

---

## 🔍 PHẠM VI HOẠT ĐỘNG CỦA EXCEPTION HANDLER

### ✅ **`@ExceptionHandler(Exception.class)` sẽ bắt:**

- ✅ Mọi exception từ **Controller layer** (không được catch)
- ✅ Exception được throw từ Service/Repository lên Controller
- ✅ Mọi exception **KHÔNG** được xử lý bởi handler cụ thể khác
- ✅ Là "safety net" (lưới an toàn) cuối cùng

### ❌ **KHÔNG bắt:**

- ❌ Exception đã được catch trong code
- ❌ Exception trong async/scheduled task
- ❌ Exception trong filter (tuỳ cấu hình)
- ❌ Exception ngoài web layer

### 🎯 **Thứ tự ưu tiên của Exception Handler:**

Spring sẽ tìm handler **CỤ THỂ NHẤT** trước:

```java
@ControllerAdvice
public class ExceptionAdvice {

    // 1. Ưu tiên CAO NHẤT - Exception cụ thể
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(...) {
        // Validation error → VÀO ĐÂY
    }

    // 2. Ưu tiên VỪA - Exception trung gian
    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<?> handleCompletion(...) {
        // Axon exception → VÀO ĐÂY
    }

    // 3. Ưu tiên THẤP NHẤT - Exception tổng quát
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(...) {
        // Tất cả exception KHÁC không match 1,2 → VÀO ĐÂY
    }
}
```

**Ví dụ cụ thể:**

- Validation error → `MethodArgumentNotValidException` → vào handler 1
- Axon error → `CompletionException` → vào handler 2
- NullPointerException, IllegalArgumentException → vào handler 3
- RuntimeException tự định nghĩa → vào handler 3

**→ Đây là cơ chế "catch-all" cho Controller layer, không phải toàn bộ application!**

---

## ⚙️ WORKFLOW CẬP NHẬT COMMON SERVICE

**Khi cần sửa logic trong commonservice:**

### Bước 1: Sửa code trong commonservice

```bash
# Mở file cần sửa
vim commonservice/src/main/java/com/cinema/commonservice/advice/ExceptionAdvice.java
```

### Bước 2: Build và install commonservice

```bash
cd commonservice
mvnw clean install -DskipTests
```

### Bước 3: Rebuild tất cả service sử dụng

```bash
cd ../movies
mvnw clean package -DskipTests

cd ../cinema
mvnw clean package -DskipTests
```

### Bước 4: Restart các service

Thay đổi sẽ được áp dụng cho tất cả service!

---

## 🛠️ TROUBLESHOOTING

### **Vấn đề 1: Validation không hoạt động**

**Triệu chứng:** Gửi request thiếu field nhưng không trả về error validation

**Nguyên nhân:**

- Thiếu `@Valid` trong Controller
- Thiếu validation annotation trong Model
- ExceptionAdvice không được scan

**Giải pháp:**

1. Kiểm tra `@Valid` trong Controller method
2. Kiểm tra `@ComponentScan` có bao gồm `com.cinema.commonservice`
3. Kiểm tra groupId trong pom.xml phải đúng
4. Rebuild commonservice và movies service

### **Vấn đề 2: Sai groupId trong pom.xml**

**Triệu chứng:** Build failed hoặc ExceptionAdvice không được load

**Ví dụ lỗi:**

```xml
<!-- SAI -->
<dependency>
    <groupId>com.nguyenthanhhoan</groupId>
    <artifactId>commonservice</artifactId>
</dependency>

<!-- ĐÚNG -->
<dependency>
    <groupId>com.cinema</groupId>
    <artifactId>commonservice</artifactId>
</dependency>
```

**Giải pháp:** Sửa groupId cho khớp với commonservice/pom.xml

### **Vấn đề 3: Exception không được format đúng**

**Triệu chứng:** Vẫn nhận được error mặc định của Spring thay vì format custom

**Nguyên nhân:** ExceptionAdvice chưa được scan hoặc chưa install commonservice

**Giải pháp:**

```bash
# Build lại commonservice
cd commonservice
mvnw clean install -DskipTests

# Build lại movies
cd ../movies
mvnw clean package -DskipTests

# Restart application
```

---

## 🎓 KẾT LUẬN

**Common Service là:**

- ✅ Thư viện dùng chung cho tất cả microservice
- ✅ Chứa logic xử lý exception toàn cục
- ✅ Đảm bảo tính nhất quán trong error handling
- ✅ Giảm code trùng lặp, dễ bảo trì
- ✅ Có thể mở rộng thêm nhiều utility khác

**Trong project của bạn:**

- `ExceptionAdvice` tự động bắt exception từ tất cả Controller
- `ErrorMessage` cung cấp format chuẩn cho error response
- Tất cả service (movies, cinema, etc.) đều hưởng lợi từ common service

**→ Đây là best practice trong microservices architecture!**

---

---

# 📨 TÀI LIỆU KAFKA INTEGRATION

## 🎯 KAFKA LÀ GÌ?

**Apache Kafka** là một **distributed streaming platform** (nền tảng phân tán xử lý luồng dữ liệu) được sử dụng để:

- **Publish/Subscribe**: Gửi và nhận messages giữa các service
- **Event Streaming**: Xử lý luồng sự kiện real-time
- **Message Queue**: Hàng đợi tin nhắn bất đồng bộ
- **Event-Driven Architecture**: Kiến trúc hướng sự kiện

**Trong project của bạn:**

- Kafka được tích hợp vào `commonservice` như một shared component
- Sử dụng **KRaft mode** (không cần ZooKeeper)
- Tất cả các service có thể gửi và nhận messages thông qua Kafka
- Mục đích: **Giao tiếp bất đồng bộ giữa các microservice**

---

## 🏗️ KIẾN TRÚC KAFKA TRONG PROJECT

```
┌─────────────────────────────────────────────────────────────┐
│                     KAFKA CLUSTER (Docker)                  │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Kafka Broker (Port 9092 internal, 9094 external) │    │
│  │  - Auto-create topics                              │    │
│  │  - 3 partitions per topic                          │    │
│  │  - Replication factor: 1                           │    │
│  └────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Kafka UI (Port 5678)                              │    │
│  │  - Monitor topics, messages, consumers             │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│                    COMMON SERVICE                           │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │  KafkaConfig     │←────────│  KafkaService    │         │
│  │  - Producer      │         │  - sendMessage() │         │
│  │  - Consumer      │         └──────────────────┘         │
│  └──────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│            MICROSERVICES (Movies, Cinema, Seat, etc.)       │
│  - Import commonservice                                     │
│  - Autowire KafkaService                                    │
│  - Send/Receive messages                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 CẤU TRÚC KAFKA INTEGRATION

```
commonservice/
├── src/main/java/com/cinema/commonservice/
│   ├── configuration/
│   │   └── KafkaConfig.java              # Cấu hình Kafka Producer/Consumer
│   └── service/
│       └── KafkaService.java              # Service gửi messages
├── src/main/resources/
│   └── application.yml                    # Kafka configuration
└── pom.xml                                # Kafka dependencies

docker-kafka.yml                           # Docker compose cho Kafka
```

---

## 🔧 CÁC THÀNH PHẦN KAFKA

### **1. KafkaConfig.java** - Cấu hình Producer và Consumer

#### **Tổng quan:**

KafkaConfig định nghĩa các bean cần thiết để:

- **Producer**: Gửi messages đến Kafka topic
- **Consumer**: Nhận messages từ Kafka topic
- **Serializer/Deserializer**: Chuyển đổi dữ liệu Java ↔ Kafka message

#### **Chi tiết các thành phần:**

##### **a) Producer Configuration**

```java
@Bean
public ProducerFactory<String, String> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(configProps);
}
```

**Giải thích:**

- **BOOTSTRAP_SERVERS_CONFIG**: Địa chỉ Kafka broker (`localhost:9092`)
- **KEY_SERIALIZER_CLASS_CONFIG**: Cách serialize key thành bytes (String → bytes)
- **VALUE_SERIALIZER_CLASS_CONFIG**: Cách serialize message thành bytes (String → bytes)

**Tại sao cần Serializer?**

- Kafka chỉ truyền dữ liệu dưới dạng **bytes**
- Java String cần được chuyển thành bytes trước khi gửi
- `StringSerializer` làm việc này tự động

##### **b) KafkaTemplate Bean**

```java
@Bean
public KafkaTemplate<String, String> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
}
```

**Giải thích:**

- `KafkaTemplate` là **high-level API** để gửi messages
- Wrap `ProducerFactory` để dễ sử dụng
- Tự động handle connection pooling, error handling

##### **c) Consumer Configuration**

```java
@Bean
public ConsumerFactory<String, String> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(props);
}
```

**Giải thích:**

- **GROUP_ID_CONFIG**: Consumer group ID (quan trọng cho load balancing)
- **KEY/VALUE_DESERIALIZER**: Chuyển bytes → String

**Consumer Group là gì?**

- Nhóm các consumer cùng đọc messages từ một topic
- Kafka phân chia partitions cho các consumer trong group
- Đảm bảo mỗi message chỉ được xử lý bởi 1 consumer trong group

##### **d) Kafka Listener Container Factory**

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
}
```

**Giải thích:**

- Factory tạo **listener containers** để nhận messages
- **Concurrent**: Hỗ trợ nhiều thread xử lý song song
- Cần thiết cho `@KafkaListener` annotation

---

### **2. KafkaService.java** - Service Gửi Messages

```java
@Service
@Slf4j
public class KafkaService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
        log.info("Message send to topic : " + topic);
    }
}
```

**Tác dụng:**

- Wrapper đơn giản cho `KafkaTemplate`
- Cung cấp method tiện lợi để gửi message
- Tự động log khi gửi message

**Tại sao không dùng KafkaTemplate trực tiếp?**

- ✅ **Abstraction**: Che giấu chi tiết implementation
- ✅ **Logging**: Tự động log mọi message
- ✅ **Error handling**: Có thể thêm retry logic, error handling
- ✅ **Testing**: Dễ mock KafkaService hơn KafkaTemplate

---

### **3. application.yml** - Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:broker:9092}
    consumer:
      group-id: ${KAFKA_GROUP_ID:default-group}
```

**Giải thích:**

- **bootstrap-servers**: Địa chỉ Kafka broker
  - Mặc định: `broker:9092` (trong Docker network)
  - Override bằng environment variable `KAFKA_BOOTSTRAP_SERVERS`
- **consumer.group-id**: Consumer group ID
  - Mặc định: `default-group`
  - Override bằng `KAFKA_GROUP_ID`

**Best Practice:**

- ✅ Dùng environment variables cho flexibility
- ✅ Mỗi service nên có consumer group riêng
- ✅ Ví dụ: `movies-service-group`, `cinema-service-group`

---

### **4. docker-kafka.yml** - Docker Compose

#### **Kafka Broker Configuration:**

```yaml
kafka:
  image: apache/kafka
  ports:
    - "9092:9092" # INTERNAL (Docker network)
    - "9094:9094" # EXTERNAL (Host)
  environment:
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_NODE_ID: 1
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
```

**Giải thích:**

- **KRaft mode**: Kafka chạy mà không cần ZooKeeper
- **broker,controller**: Kafka vừa là broker vừa là controller
- **Port 9092**: Cho services trong Docker network
- **Port 9094**: Cho applications chạy ở host (localhost)

#### **Topic Configuration:**

```yaml
KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
KAFKA_NUM_PARTITIONS: 3
KAFKA_DEFAULT_REPLICATION_FACTOR: 1
```

**Giải thích:**

- **AUTO_CREATE_TOPICS**: Tự động tạo topic khi gửi message lần đầu
- **NUM_PARTITIONS**: Mỗi topic có 3 partitions (tăng throughput)
- **REPLICATION_FACTOR**: 1 replica (đủ cho development)

#### **Kafka UI:**

```yaml
kafka-ui:
  image: provectuslabs/kafka-ui:latest
  ports:
    - "5678:8080"
  environment:
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

**Truy cập:** `http://localhost:5678`

**Chức năng:**

- ✅ Xem danh sách topics
- ✅ Monitor messages trong topic
- ✅ Xem consumer groups
- ✅ Xem partitions và offsets

---

## 🚀 CÁCH SỬ DỤNG KAFKA SERVICE

### **Bước 1: Import commonservice vào service khác**

**Trong movies/pom.xml:**

```xml
<dependency>
    <groupId>com.cinema</groupId>
    <artifactId>commonservice</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### **Bước 2: Scan commonservice package**

**Trong MoviesApplication.java:**

```java
@SpringBootApplication
@ComponentScan({ "com.cinema.movies", "com.cinema.commonservice" })
public class MoviesApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoviesApplication.class, args);
    }
}
```

### **Bước 3: Cấu hình Kafka trong application.yml**

**Trong movies/src/main/resources/application.yml:**

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9094 # Sử dụng port 9094 (external)
    consumer:
      group-id: movies-service-group
```

### **Bước 4: Gửi Message (Producer)**

**Trong MoviesService.java:**

```java
@Service
public class MoviesService {

    @Autowired
    private KafkaService kafkaService;

    public void createMovie(MovieRequestModel model) {
        // Business logic
        Movie movie = movieRepository.save(...);

        // Gửi event đến Kafka
        String message = "Movie created: " + movie.getId();
        kafkaService.sendMessage("movie-events", message);
    }
}
```

### **Bước 5: Nhận Message (Consumer)**

**Trong MoviesEventConsumer.java:**

```java
@Service
@Slf4j
public class MoviesEventConsumer {

    @KafkaListener(topics = "movie-events", groupId = "movies-service-group")
    public void handleMovieEvent(String message) {
        log.info("Received message: {}", message);

        // Xử lý message
        // Ví dụ: Update cache, notify other services, etc.
    }
}
```

**Giải thích:**

- `@KafkaListener`: Annotation để đánh dấu method nhận message
- **topics**: Topic cần listen
- **groupId**: Consumer group (phải khớp với config)
- Method tự động được gọi khi có message mới

---

## 📋 WORKFLOW KAFKA MESSAGE FLOW

### **Scenario: Tạo Movie và Notify Các Service Khác**

#### **Bước 1: Client gửi request tạo movie**

```http
POST http://localhost:8081/api/v1/movies
Content-Type: application/json

{
  "title": "Avengers Endgame",
  "description": "Marvel movie",
  "duration": 180
}
```

#### **Bước 2: MoviesService xử lý request**

```java
@Service
public class MoviesService {

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private MovieRepository movieRepository;

    public String createMovie(MovieRequestModel model) {
        // 1. Lưu movie vào database
        Movie movie = new Movie();
        movie.setTitle(model.getTitle());
        movieRepository.save(movie);

        // 2. Gửi event đến Kafka
        String eventMessage = String.format(
            "{'eventType': 'MOVIE_CREATED', 'movieId': '%s', 'title': '%s'}",
            movie.getId(), movie.getTitle()
        );
        kafkaService.sendMessage("movie-events", eventMessage);

        return movie.getId();
    }
}
```

#### **Bước 3: KafkaService gửi message đến Kafka broker**

```java
// KafkaService.sendMessage() internally:
kafkaTemplate.send("movie-events", eventMessage);
// → Message được gửi đến Kafka topic "movie-events"
```

#### **Bước 4: Kafka broker nhận và lưu message**

```
Kafka Broker:
└── Topic: "movie-events"
    ├── Partition 0: [message 1]
    ├── Partition 1: [message 2] ← message mới vào đây
    └── Partition 2: []
```

#### **Bước 5: Consumer ở các service khác nhận message**

**Trong cinema-service:**

```java
@Service
@Slf4j
public class CinemaEventConsumer {

    @KafkaListener(topics = "movie-events", groupId = "cinema-service-group")
    public void handleMovieEvent(String message) {
        log.info("Cinema service received: {}", message);

        // Parse message
        // Update local cache hoặc database
        // Ví dụ: Cập nhật danh sách phim khả dụng
    }
}
```

**Trong notification-service:**

```java
@Service
@Slf4j
public class NotificationEventConsumer {

    @KafkaListener(topics = "movie-events", groupId = "notification-service-group")
    public void handleMovieEvent(String message) {
        log.info("Notification service received: {}", message);

        // Gửi email thông báo phim mới
        // Gửi push notification
    }
}
```

#### **Bước 6: Response trả về Client**

```json
HTTP/1.1 200 OK
Content-Type: text/plain

"movie-123"
```

**→ Message đã được gửi bất đồng bộ đến tất cả services quan tâm!**

---

## 🎯 CÁC PATTERN SỬ DỤNG KAFKA

### **1. Event Notification Pattern**

**Use case:** Thông báo sự kiện đã xảy ra

**Ví dụ:**

```java
// Movies Service
kafkaService.sendMessage("movie-events", "MOVIE_CREATED: movie-123");

// Cinema Service nhận và cập nhật cache
@KafkaListener(topics = "movie-events")
public void updateLocalCache(String event) {
    // Refresh local movie list
}
```

**Đặc điểm:**

- ✅ Decoupling: Service không biết ai sẽ nhận message
- ✅ Scalability: Dễ thêm consumer mới
- ✅ Fire-and-forget: Không cần chờ response

---

### **2. Event-Carried State Transfer Pattern**

**Use case:** Truyền toàn bộ state trong event

**Ví dụ:**

```java
// Movies Service
String event = """
{
    "eventType": "MOVIE_CREATED",
    "movieId": "movie-123",
    "title": "Avengers",
    "description": "Marvel movie",
    "duration": 180,
    "timestamp": "2026-01-18T10:00:00Z"
}
""";
kafkaService.sendMessage("movie-events", event);

// Cinema Service nhận đầy đủ thông tin, không cần query lại
@KafkaListener(topics = "movie-events")
public void handleMovieCreated(String event) {
    MovieEvent movieEvent = objectMapper.readValue(event, MovieEvent.class);
    // Có đầy đủ thông tin, không cần gọi API movies service
    localMovieCache.put(movieEvent.getMovieId(), movieEvent);
}
```

**Đặc điểm:**

- ✅ Giảm coupling: Consumer không cần gọi API producer
- ✅ Offline capability: Consumer có đủ data để xử lý
- ❌ Message size lớn hơn

---

### **3. CQRS (Command Query Responsibility Segregation) Pattern**

**Use case:** Tách read model và write model

**Ví dụ:**

```java
// Write model (Movies Service)
@CommandHandler
public void handle(CreateMovieCommand command) {
    // Lưu vào write database
    movieRepository.save(movie);

    // Publish event
    kafkaService.sendMessage("movie-events", event);
}

// Read model (Cinema Service)
@KafkaListener(topics = "movie-events")
public void updateReadModel(String event) {
    // Cập nhật read database (optimized for queries)
    readMovieRepository.save(optimizedMovie);
}
```

**Đặc điểm:**

- ✅ Optimize read và write riêng biệt
- ✅ Scale read và write độc lập
- ❌ Eventual consistency (có delay)

---

### **4. Saga Pattern - Event Choreography**

**Use case:** Distributed transaction với event-driven

**Ví dụ: Booking workflow**

```java
// 1. Booking Service: Tạo booking
kafkaService.sendMessage("booking-events", "BOOKING_CREATED: booking-123");

// 2. Seat Service: Reserve seats
@KafkaListener(topics = "booking-events")
public void reserveSeats(String event) {
    seatRepository.reserveSeats(seatIds);
    kafkaService.sendMessage("seat-events", "SEATS_RESERVED: booking-123");
}

// 3. Payment Service: Process payment
@KafkaListener(topics = "seat-events")
public void processPayment(String event) {
    paymentService.charge(amount);
    kafkaService.sendMessage("payment-events", "PAYMENT_SUCCESS: booking-123");
}

// 4. Notification Service: Send confirmation
@KafkaListener(topics = "payment-events")
public void sendConfirmation(String event) {
    emailService.sendBookingConfirmation(booking);
}
```

**Đặc điểm:**

- ✅ Decentralized: Không có orchestrator
- ✅ Loose coupling
- ❌ Khó debug, khó trace flow

---

## 🔄 KAFKA VS REST API

### **Khi nào dùng Kafka?**

✅ **Event notification**: Thông báo sự kiện đã xảy ra
✅ **Asynchronous processing**: Xử lý không cần response ngay
✅ **Fan-out**: 1 message → nhiều consumers
✅ **Decoupling**: Services không biết nhau
✅ **Event sourcing**: Lưu lại tất cả events
✅ **High throughput**: Cần xử lý hàng triệu messages

**Ví dụ:**

- User đăng ký → gửi email, update analytics, sync CRM
- Order created → update inventory, notify warehouse, send invoice
- Movie created → update cache, notify cinemas, send notification

### **Khi nào dùng REST API?**

✅ **Synchronous request/response**: Cần kết quả ngay
✅ **CRUD operations**: Đơn giản, trực tiếp
✅ **Request/Reply pattern**: Client cần response
✅ **Low latency**: Cần response < 100ms

**Ví dụ:**

- Get movie details → trả về ngay
- Login → trả về token ngay
- Search movies → trả về kết quả ngay

### **Kết hợp cả hai:**

```java
@PostMapping("/movies")
public ResponseEntity<String> createMovie(@RequestBody MovieRequestModel model) {
    // 1. Synchronous: Lưu vào database
    String movieId = movieService.createMovie(model);

    // 2. Asynchronous: Notify other services via Kafka
    kafkaService.sendMessage("movie-events", "MOVIE_CREATED: " + movieId);

    // 3. Return response ngay cho client
    return ResponseEntity.ok(movieId);
}
```

---

## ⚙️ CẤU HÌNH KAFKA CHO PRODUCTION

### **1. Producer Configuration**

```java
@Bean
public ProducerFactory<String, String> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    // Production settings
    configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Đảm bảo message được replicate
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // Retry 3 lần nếu fail
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // Batch size 16KB
    configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Chờ 10ms để batch messages
    configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Nén message

    return new DefaultKafkaProducerFactory<>(configProps);
}
```

### **2. Consumer Configuration**

```java
@Bean
public ConsumerFactory<String, String> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    // Production settings
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Đọc từ đầu nếu chưa có offset
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100); // Xử lý 100 messages mỗi lần

    return new DefaultKafkaConsumerFactory<>(props);
}
```

### **3. Error Handling**

```java
@KafkaListener(topics = "movie-events", groupId = "cinema-service-group")
public void handleMovieEvent(String message) {
    try {
        // Process message
        processMessage(message);
    } catch (Exception e) {
        log.error("Failed to process message: {}", message, e);

        // Option 1: Gửi đến Dead Letter Queue (DLQ)
        kafkaService.sendMessage("movie-events-dlq", message);

        // Option 2: Retry với exponential backoff
        // Option 3: Log và skip
    }
}
```

### **4. Monitoring**

**Metrics cần theo dõi:**

- ✅ Producer send rate
- ✅ Consumer lag (offset lag)
- ✅ Partition count
- ✅ Replication status
- ✅ Error rate

**Tools:**

- Kafka UI (http://localhost:5678)
- Prometheus + Grafana
- Kafka Manager

---

## 🛠️ TROUBLESHOOTING

### **Vấn đề 1: Consumer không nhận được message**

**Triệu chứng:**

- Producer gửi message thành công
- Consumer không log ra message

**Nguyên nhân:**

1. Consumer group ID không khớp
2. Topic name sai
3. Consumer chưa start
4. Consumer offset đã vượt qua message

**Giải pháp:**

```bash
# Check topic trong Kafka UI
http://localhost:5678

# Hoặc dùng Kafka CLI
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh \
  --list --bootstrap-server localhost:9092

# Check consumer group
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --list --bootstrap-server localhost:9092

# Reset consumer offset về đầu
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group movies-service-group \
  --topic movie-events \
  --reset-offsets --to-earliest --execute
```

---

### **Vấn đề 2: Connection refused**

**Triệu chứng:**

```
org.apache.kafka.common.errors.TimeoutException:
  Failed to update metadata after 60000 ms.
```

**Nguyên nhân:**

- Kafka container chưa start
- Port mapping sai
- Bootstrap servers config sai

**Giải pháp:**

```bash
# Check Kafka container
docker ps | grep kafka

# Check logs
docker logs kafka

# Restart Kafka
docker-compose -f docker-kafka.yml restart

# Trong application.yml, đảm bảo:
spring:
  kafka:
    bootstrap-servers: localhost:9094  # Port 9094 cho external
```

---

### **Vấn đề 3: Message bị duplicate**

**Triệu chứng:**

- Consumer xử lý cùng message nhiều lần

**Nguyên nhân:**

- Consumer crash trước khi commit offset
- Auto-commit enabled với xử lý chậm

**Giải pháp:**

```java
// 1. Idempotent processing
@KafkaListener(topics = "movie-events")
public void handleMovieEvent(String message) {
    String messageId = extractMessageId(message);

    // Check if already processed
    if (processedMessages.contains(messageId)) {
        log.info("Message already processed: {}", messageId);
        return;
    }

    // Process
    processMessage(message);

    // Mark as processed
    processedMessages.add(messageId);
}

// 2. Manual commit
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
    factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
    return factory;
}

@KafkaListener(topics = "movie-events")
public void handleMovieEvent(String message, Acknowledgment ack) {
    processMessage(message);
    ack.acknowledge(); // Commit sau khi xử lý xong
}
```

---

### **Vấn đề 4: Consumer lag cao**

**Triệu chứng:**

- Kafka UI hiển thị lag > 1000 messages
- Message xử lý chậm

**Nguyên nhân:**

- Processing logic chậm
- Không đủ consumer instances
- Network latency

**Giải pháp:**

```java
// 1. Increase concurrency
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
    factory.setConcurrency(3); // 3 threads xử lý song song
    return factory;
}

// 2. Optimize processing
@KafkaListener(topics = "movie-events")
@Async // Xử lý async
public void handleMovieEvent(String message) {
    // Batch processing
    // Cache frequently accessed data
    // Reduce external API calls
}

// 3. Scale consumer instances
# Deploy nhiều instances của service với cùng consumer group
```

---

## 🎓 BEST PRACTICES

### **1. Topic Naming Convention**

```
{service}-{entity}-{event-type}
```

**Ví dụ:**

- `movies-movie-created`
- `cinema-showtime-updated`
- `booking-payment-completed`

### **2. Message Format**

**Sử dụng JSON với schema rõ ràng:**

```java
public class MovieEvent {
    private String eventId;        // UUID
    private String eventType;      // MOVIE_CREATED, MOVIE_UPDATED, etc.
    private String movieId;
    private MovieData data;        // Full movie data
    private LocalDateTime timestamp;
    private String version;        // Schema version (v1, v2)
}
```

### **3. Error Handling Strategy**

```java
@KafkaListener(topics = "movie-events")
public void handleMovieEvent(String message) {
    try {
        processMessage(message);
    } catch (BusinessException e) {
        // Business error → log và skip
        log.warn("Business error: {}", e.getMessage());
    } catch (TransientException e) {
        // Transient error → retry
        throw e; // Let Kafka retry
    } catch (Exception e) {
        // Unknown error → DLQ
        kafkaService.sendMessage("movie-events-dlq", message);
    }
}
```

### **4. Idempotency**

**Đảm bảo xử lý message nhiều lần không gây lỗi:**

```java
@KafkaListener(topics = "movie-events")
@Transactional
public void handleMovieEvent(String message) {
    MovieEvent event = parseMessage(message);

    // Upsert instead of insert
    movieRepository.save(event.getMovie()); // UPDATE nếu đã tồn tại

    // Hoặc check trước
    if (!movieRepository.existsById(event.getMovieId())) {
        movieRepository.save(event.getMovie());
    }
}
```

### **5. Logging và Monitoring**

```java
@KafkaListener(topics = "movie-events")
public void handleMovieEvent(String message) {
    long startTime = System.currentTimeMillis();

    try {
        log.info("Processing message: topic={}, offset={}, key={}",
            record.topic(), record.offset(), record.key());

        processMessage(message);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Message processed successfully in {}ms", duration);

        // Send metrics
        metricsService.recordProcessingTime("movie-events", duration);

    } catch (Exception e) {
        log.error("Failed to process message: {}", message, e);
        metricsService.incrementErrorCount("movie-events");
        throw e;
    }
}
```

---

## 🚀 WORKFLOW SETUP KAFKA

### **Bước 1: Start Kafka**

```bash
# Start Kafka và Kafka UI
docker-compose -f docker-kafka.yml up -d

# Check logs
docker logs -f kafka

# Verify
docker ps | grep kafka
```

### **Bước 2: Build commonservice**

```bash
cd commonservice
mvnw clean install -DskipTests
```

### **Bước 3: Configure service sử dụng Kafka**

**Trong movies/application.yml:**

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9094
    consumer:
      group-id: movies-service-group
```

### **Bước 4: Build và start service**

```bash
cd ../movies
mvnw clean package -DskipTests
java -jar target/movies-0.0.1-SNAPSHOT.jar
```

### **Bước 5: Test gửi message**

```java
// Trong controller hoặc service
@Autowired
private KafkaService kafkaService;

@PostMapping("/test-kafka")
public String testKafka() {
    kafkaService.sendMessage("test-topic", "Hello Kafka!");
    return "Message sent!";
}
```

### **Bước 6: Verify trong Kafka UI**

1. Mở http://localhost:5678
2. Click vào topic "test-topic"
3. Xem message vừa gửi

---

## 📊 SO SÁNH KAFKA VS RabbitMQ

| Feature               | Kafka                  | RabbitMQ                    |
| --------------------- | ---------------------- | --------------------------- |
| **Architecture**      | Distributed log        | Message broker              |
| **Throughput**        | Rất cao (millions/sec) | Trung bình (thousands/sec)  |
| **Latency**           | 10-50ms                | 1-10ms                      |
| **Message retention** | Có (lưu lâu dài)       | Không (xóa sau khi consume) |
| **Use case**          | Event streaming, logs  | Task queues, RPC            |
| **Replay capability** | Có                     | Không                       |
| **Learning curve**    | Khó hơn                | Dễ hơn                      |

**→ Kafka tốt cho event-driven architecture, RabbitMQ tốt cho task queues!**

---

## 🎯 KẾT LUẬN

**Kafka Integration trong Common Service cung cấp:**

- ✅ **Shared Kafka configuration** cho tất cả microservices
- ✅ **Producer và Consumer factory** đã được cấu hình sẵn
- ✅ **KafkaService** đơn giản để gửi messages
- ✅ **Docker setup** với Kafka UI để monitoring
- ✅ **Decoupling** giữa các services
- ✅ **Asynchronous communication** với high throughput

**Lợi ích:**

- 🚀 Giao tiếp bất đồng bộ giữa microservices
- 🔄 Event-driven architecture
- 📊 High throughput và scalability
- 🛡️ Fault tolerance (messages không bị mất)
- 🔍 Dễ debug với Kafka UI

**→ Kafka là backbone cho event-driven microservices architecture!**

---

## 📚 TÀI LIỆU THAM KHẢO

- [Spring Boot Exception Handling](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc)
- [Bean Validation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)
- [@ControllerAdvice Documentation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/ControllerAdvice.html)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Kafka: The Definitive Guide](https://www.confluent.io/resources/kafka-the-definitive-guide/)
- [Event-Driven Architecture](https://martinfowler.com/articles/201701-event-driven.html)
