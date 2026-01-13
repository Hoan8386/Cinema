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

## 📚 TÀI LIỆU THAM KHẢO

- [Spring Boot Exception Handling](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc)
- [Bean Validation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)
- [@ControllerAdvice Documentation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/ControllerAdvice.html)
