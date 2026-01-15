# TÀI LIỆU CHI TIẾT MOVIES SERVICE

## 📋 Mục Lục

1. [Tổng Quan](#tổng-quan)
2. [Kiến Trúc Hệ Thống](#kiến-trúc-hệ-thống)
3. [Công Nghệ Sử Dụng](#công-nghệ-sử-dụng)
4. [Cấu Trúc Dự Án](#cấu-trúc-dự-án)
5. [Mô Hình Dữ Liệu](#mô-hình-dữ-liệu)
6. [Command Side (Ghi Dữ Liệu)](#command-side-ghi-dữ-liệu)
7. [Query Side (Đọc Dữ Liệu)](#query-side-đọc-dữ-liệu)
8. [API Endpoints](#api-endpoints)
9. [Cấu Hình](#cấu-hình)
10. [Hướng Dẫn Cài Đặt](#hướng-dẫn-cài-đặt)
11. [Testing](#testing)
12. [Best Practices](#best-practices)

---

## Tổng Quan

**Movies Service** là một microservice trong hệ thống Cinema Microservices, được xây dựng để quản lý toàn bộ nghiệp vụ liên quan đến:

- **Movies (Phim)**: Quản lý thông tin phim
- **Cinemas (Rạp chiếu)**: Quản lý thông tin rạp
- **Seats (Ghế ngồi)**: Quản lý ghế trong từng rạp
- **ShowTimes (Suất chiếu)**: Quản lý lịch chiếu phim
- **Employees (Nhân viên)**: Quản lý thông tin nhân viên rạp
- **WorkShifts (Ca làm việc)**: Quản lý ca làm việc và điểm danh nhân viên

### Đặc điểm chính:

- ✅ **CQRS Pattern**: Tách biệt luồng Command (ghi) và Query (đọc)
- ✅ **Event Sourcing**: Lưu trữ trạng thái thông qua các sự kiện
- ✅ **Microservices Architecture**: Độc lập, có thể scale riêng biệt
- ✅ **Service Discovery**: Đăng ký với Eureka Server
- ✅ **RESTful API**: API chuẩn REST cho tích hợp dễ dàng

---

## Kiến Trúc Hệ Thống

### CQRS và Event Sourcing với Axon Framework

```
┌─────────────────────────────────────────────────────────────┐
│                      MOVIES SERVICE                          │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌────────────────┐              ┌────────────────┐         │
│  │  Command Side  │              │   Query Side   │         │
│  │   (Write)      │              │    (Read)      │         │
│  ├────────────────┤              ├────────────────┤         │
│  │                │              │                │         │
│  │ Controllers    │              │ Controllers    │         │
│  │      ↓         │              │      ↓         │         │
│  │ Commands       │              │ Queries        │         │
│  │      ↓         │              │      ↓         │         │
│  │ Aggregates     │   Events     │ Projections    │         │
│  │      ↓         │──────────────→│      ↓         │         │
│  │ Event Store    │              │ Read Database  │         │
│  │                │              │   (H2/JPA)     │         │
│  └────────────────┘              └────────────────┘         │
│                                                               │
└─────────────────────────────────────────────────────────────┘
          ↓                                    ↑
          └────────── Eureka Client ──────────┘
                     (Service Registry)
```

### Luồng Xử Lý

#### Command Flow (Ghi):

1. **Client** gửi HTTP Request → **Command Controller**
2. **Command Controller** tạo **Command** object
3. **Command** được gửi qua **CommandGateway** (Axon)
4. **CommandHandler** trong **Aggregate** xử lý Command
5. **Aggregate** tạo **Event** và publish
6. **Event** được lưu vào **Event Store**
7. **EventHandler** trong **Projection** lắng nghe và cập nhật Read Database

#### Query Flow (Đọc):

1. **Client** gửi HTTP Request → **Query Controller**
2. **Query Controller** tạo **Query** object
3. **Query** được gửi qua **QueryGateway** (Axon)
4. **QueryHandler** trong **Projection** xử lý Query
5. Truy vấn dữ liệu từ **Read Database** (H2)
6. Trả về **Response Model** cho Client

---

## Công Nghệ Sử Dụng

### Core Technologies

| Công nghệ          | Version  | Mục đích              |
| ------------------ | -------- | --------------------- |
| **Spring Boot**    | 4.0.1    | Framework chính       |
| **Java**           | 17       | Ngôn ngữ lập trình    |
| **Axon Framework** | 4.9.3    | CQRS & Event Sourcing |
| **Spring Cloud**   | 2025.1.0 | Microservices support |

### Dependencies

| Dependency                                   | Mục đích              |
| -------------------------------------------- | --------------------- |
| `spring-boot-starter-webmvc`                 | REST API              |
| `spring-boot-starter-data-jpa`               | Database access       |
| `axon-spring-boot-starter`                   | Axon integration      |
| `spring-cloud-starter-netflix-eureka-client` | Service discovery     |
| `h2`                                         | In-memory database    |
| `lombok`                                     | Giảm boilerplate code |

---

## Cấu Trúc Dự Án

```
movies/
├── src/main/java/com/cinema/movies/
│   ├── MoviesApplication.java           # Entry point
│   │
│   ├── command/                         # COMMAND SIDE (Write)
│   │   ├── aggregate/                   # Aggregates - Business logic
│   │   │   ├── MovieAggregate.java
│   │   │   ├── CinemaAggregate.java
│   │   │   ├── SeatAggregate.java
│   │   │   └── ShowTimeAggregate.java
│   │   │
│   │   ├── command/                     # Command objects
│   │   │   ├── CreateMovieCommand.java
│   │   │   ├── UpdateMovieCommand.java
│   │   │   ├── DeleteMovieCommand.java
│   │   │   └── ... (các command khác)
│   │   │
│   │   ├── controller/                  # REST Controllers
│   │   │   ├── MovieCommandController.java
│   │   │   ├── CinemaCommandController.java
│   │   │   ├── SeatCommandController.java
│   │   │   └── ShowTimeCommandController.java
│   │   │
│   │   ├── data/                        # JPA Entities
│   │   │   ├── Movie.java
│   │   │   ├── Cinema.java
│   │   │   ├── Seat.java
│   │   │   ├── ShowTime.java
│   │   │   └── Reponsitory/            # Spring Data Repositories
│   │   │       ├── MovieRepository.java
│   │   │       ├── CinemaRepository.java
│   │   │       ├── SeatRepository.java
│   │   │       └── ShowTimeRepository.java
│   │   │
│   │   ├── event/                       # Event objects
│   │   │   ├── MovieCreateEvent.java
│   │   │   ├── MovieUpdatedEvent.java
│   │   │   ├── MovieDeletedEvent.java
│   │   │   └── ... (các event khác)
│   │   │
│   │   └── model/                       # Request/Response models
│   │       ├── MovieRequestModel.java
│   │       ├── CinemaRequestModel.java
│   │       ├── SeatRequestModel.java
│   │       ├── ShowTimeRequestModel.java
│   │       └── CommandResponse.java
│   │
│   └── query/                           # QUERY SIDE (Read)
│       ├── controller/                  # REST Controllers
│       │   ├── MovieQueryController.java
│       │   ├── CinemaQueryController.java
│       │   ├── SeatQueryController.java
│       │   └── ShowTimeQueryController.java
│       │
│       ├── model/                       # Response models
│       │   ├── MovieResponseModel.java
│       │   ├── CinemaResponseModel.java
│       │   ├── SeatResponseModel.java
│       │   └── ShowTimeResponseModel.java
│       │
│       ├── projection/                  # Event Handlers & Query Handlers
│       │   ├── MovieProjection.java
│       │   ├── CinemaProjection.java
│       │   ├── SeatProjection.java
│       │   └── ShowTimeProjection.java
│       │
│       └── queries/                     # Query objects
│           ├── GetAllMoviesQuery.java
│           ├── GetMovieByIdQuery.java
│           └── ... (các query khác)
│
└── src/main/resources/
    └── application.properties           # Cấu hình ứng dụng
```

---

## Mô Hình Dữ Liệu

### 1. Movie (Phim)

```java
@Entity
@Table(name = "movies")
public class Movie {
    @Id
    private String id;              // UUID

    @Column(nullable = false)
    private String title;           // Tên phim

    @Column(columnDefinition = "TEXT")
    private String description;     // Mô tả phim

    private Integer duration;       // Thời lượng (phút)

    @Column(name = "poster_url", columnDefinition = "TEXT")
    private String posterUrl;       // URL poster phim

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // Thời gian tạo
}
```

**Ý nghĩa các trường:**

- `id`: Mã định danh duy nhất (UUID)
- `title`: Tên phim (bắt buộc)
- `description`: Mô tả nội dung phim
- `duration`: Thời lượng phim tính bằng phút
- `posterUrl`: Đường dẫn đến hình ảnh poster
- `createdAt`: Timestamp tự động khi tạo

### 2. Cinema (Rạp chiếu)

```java
@Entity
@Table(name = "cinemas")
public class Cinema {
    @Id
    private String id;              // UUID

    private String name;            // Tên rạp

    @Column(columnDefinition = "TEXT")
    private String address;         // Địa chỉ rạp
}
```

**Ý nghĩa các trường:**

- `id`: Mã định danh duy nhất
- `name`: Tên rạp chiếu
- `address`: Địa chỉ đầy đủ của rạp

### 3. Seat (Ghế ngồi)

```java
@Entity
@Table(name = "seats")
public class Seat {
    @Id
    private String id;              // UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;          // Rạp chứa ghế này

    @Column(name = "seat_row", length = 5)
    private String seatRow;         // Hàng ghế (A, B, C...)

    @Column(name = "seat_number")
    private Integer seatNumber;     // Số ghế (1, 2, 3...)
}
```

**Quan hệ:**

- Mỗi Seat thuộc về 1 Cinema (Many-to-One)

**Ý nghĩa các trường:**

- `seatRow`: Ký hiệu hàng ghế (VD: A, B, C, D)
- `seatNumber`: Số thứ tự ghế trong hàng (VD: 1, 2, 3)
- Kết hợp `seatRow` + `seatNumber` = Vị trí ghế (VD: A1, B5)

### 4. ShowTime (Suất chiếu)

```java
@Entity
@Table(name = "show_times")
public class ShowTime {
    @Id
    private String id;              // UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;            // Phim được chiếu

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;          // Rạp chiếu

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime; // Thời gian bắt đầu

    @Column(precision = 10, scale = 2)
    private BigDecimal price;       // Giá vé
}
```

**Quan hệ:**

- Mỗi ShowTime liên kết với 1 Movie (Many-to-One)
- Mỗi ShowTime liên kết với 1 Cinema (Many-to-One)

**Ý nghĩa:**

- Đại diện cho một suất chiếu cụ thể: phim gì, chiếu ở rạp nào, lúc mấy giờ, giá bao nhiêu

### 5. Employee (Nhân viên)

```java
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    private String id;              // UUID

    @Column(name = "user_id", nullable = false)
    private String userId;          // UUID của user từ User Service

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;          // Rạp làm việc

    @Column(length = 50)
    private String position;        // Vị trí: MANAGER, STAFF, TICKET_SELLER

    @Column(length = 20)
    private String status;          // ACTIVE, RESIGNED

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt; // Thời gian vào làm
}
```

**Quan hệ:**

- Mỗi Employee thuộc về 1 Cinema (Many-to-One)

**Ý nghĩa các trường:**

- `userId`: Tham chiếu đến user trong User Service
- `position`: Vị trí làm việc (Quản lý, Nhân viên, Bán vé)
- `status`: Trạng thái (Đang làm việc, Đã nghỉ)
- `joinedAt`: Ngày bắt đầu làm việc

### 6. WorkShift (Ca làm việc)

```java
@Entity
@Table(name = "work_shifts")
public class WorkShift {
    @Id
    private String id;              // UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;      // Nhân viên

    @Column(name = "shift_name", length = 50)
    private String shiftName;       // Sáng, Chiều, Tối

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime; // Thời gian bắt đầu

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;   // Thời gian kết thúc

    @Column(name = "is_attended")
    private Boolean isAttended;     // Điểm danh
}
```

**Quan hệ:**

- Mỗi WorkShift thuộc về 1 Employee (Many-to-One)

**Ý nghĩa:**

- Đại diện cho một ca làm việc: nhân viên nào, ca nào, thời gian, đã điểm danh chưa

### Sơ Đồ Quan Hệ (ERD)

```
                  ┌─────────────┐
                  │    Movie    │
                  │             │
                  │ - id        │
                  │ - title     │
                  │ - description│
                  │ - duration  │
                  │ - posterUrl │
                  │ - createdAt │
                  └─────────────┘
                         │ 1
                         │
                         │ N
                  ┌─────────────┐
                  │  ShowTime   │
                  │             │
                  │ - id        │
                  │ - movie_id  │──┐
                  │ - cinema_id │  │
                  │ - startTime │  │
                  │ - price     │  │
                  └─────────────┘  │
                         │ N       │
                         │         │
                         │ 1       │
┌─────────────┐          │         │
│   Cinema    │◄─────────┘         │
│             │                    │
│ - id        │                    │
│ - name      │                    │
│ - address   │                    │
└─────────────┘                    │
       ▲                           │
       │ 1                         │ 1
       │                           │
       ├──────────┐                │
       │ N        │ N              │
┌─────────────┐  ┌─────────────┐  │
│    Seat     │  │  Employee   │  │
│             │  │             │  │
│ - id        │  │ - id        │  │
│ - cinema_id │  │ - user_id   │  │
│ - seatRow   │  │ - cinema_id │  │
│ - seatNumber│  │ - position  │  │
└─────────────┘  │ - status    │  │
                 │ - joinedAt  │  │
                 └─────────────┘  │
                        ▲          │
                        │ 1        │
                        │          │
                        │ N        │
                 ┌─────────────┐  │
                 │  WorkShift  │  │
                 │             │  │
                 │ - id        │  │
                 │ - employee_id│  │
                 │ - shiftName │  │
                 │ - startTime │  │
                 │ - endTime   │  │
                 │ - isAttended│  │
                 └─────────────┘  │
                                  │
                                  └──(Many-to-One relationship)
```

---

## Command Side (Ghi Dữ Liệu)

### Aggregate Pattern

**Aggregate** là thành phần trung tâm trong CQRS, đại diện cho business logic và trạng thái của một entity.

#### MovieAggregate.java

```java
@Aggregate
@NoArgsConstructor
@Slf4j
public class MovieAggregate {
    @AggregateIdentifier
    private String id;
    private String title;
    private String description;
    private Integer duration;
    private String posterUrl;

    // CREATE: Command Handler
    @CommandHandler
    public MovieAggregate(CreateMovieCommand command) {
        log.info("Command received - ID: {}, Title: {}",
                 command.getId(), command.getTitle());

        // Tạo event
        MovieCreateEvent event = new MovieCreateEvent();
        BeanUtils.copyProperties(command, event);

        // Publish event
        AggregateLifecycle.apply(event);
    }

    // UPDATE: Command Handler
    @CommandHandler
    public void handle(UpdateMovieCommand command) {
        MovieUpdatedEvent event = new MovieUpdatedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

    // DELETE: Command Handler
    @CommandHandler
    public void handle(DeleteMovieCommand command) {
        MovieDeletedEvent event = new MovieDeletedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

    // Event Sourcing Handlers - Cập nhật state
    @EventSourcingHandler
    public void on(MovieCreateEvent event) {
        this.id = event.getId();
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.duration = event.getDuration();
        this.posterUrl = event.getPosterUrl();
    }

    @EventSourcingHandler
    public void on(MovieUpdatedEvent event) {
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.duration = event.getDuration();
        this.posterUrl = event.getPosterUrl();
    }

    @EventSourcingHandler
    public void on(MovieDeletedEvent event) {
        // Mark for deletion
    }
}
```

**Giải thích:**

- `@Aggregate`: Đánh dấu đây là Aggregate root
- `@AggregateIdentifier`: Định danh duy nhất của aggregate
- `@CommandHandler`: Xử lý commands từ client
- `@EventSourcingHandler`: Xử lý events để rebuild state
- `AggregateLifecycle.apply()`: Publish event vào Event Store

### Command Objects

Commands là các lệnh bất biến (immutable) thể hiện ý định thay đổi trạng thái.

#### CreateMovieCommand.java

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateMovieCommand {
    @TargetAggregateIdentifier
    private String id;

    @NotBlank(message = "Title không được để trống")
    private String title;

    private String description;

    @Min(value = 1, message = "Duration phải lớn hơn 0")
    private Integer duration;

    private String posterUrl;
}
```

**Annotations quan trọng:**

- `@TargetAggregateIdentifier`: Xác định aggregate nào sẽ xử lý command
- `@NotBlank`, `@Min`: Validation constraints

### Command Controllers

Controllers nhận HTTP requests và chuyển thành Commands.

#### MovieCommandController.java

```java
@RestController
@RequestMapping("/api/v1/movies")
@Slf4j
public class MovieCommandController {

    @Autowired
    private CommandGateway commandGateway;

    // CREATE
    @PostMapping
    @ApiMessage("Tạo phim thành công")
    public CommandResponse createMovie(@Valid @RequestBody MovieRequestModel model) {
        String id = UUID.randomUUID().toString();

        CreateMovieCommand command = new CreateMovieCommand(
            id,
            model.getTitle(),
            model.getDescription(),
            model.getDuration(),
            model.getPosterUrl()
        );

        commandGateway.sendAndWait(command);
        return new CommandResponse(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    @ApiMessage("Cập nhật phim thành công")
    public CommandResponse updateMovie(
            @PathVariable String id,
            @Valid @RequestBody MovieRequestModel model) {

        UpdateMovieCommand command = new UpdateMovieCommand(
            id,
            model.getTitle(),
            model.getDescription(),
            model.getDuration(),
            model.getPosterUrl()
        );

        commandGateway.sendAndWait(command);
        return new CommandResponse(id);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ApiMessage("Xóa phim thành công")
    public CommandResponse deleteMovie(@PathVariable String id) {
        DeleteMovieCommand command = new DeleteMovieCommand(id);
        commandGateway.sendAndWait(command);
        return new CommandResponse(id);
    }
}
```

**Luồng xử lý:**

1. Client gửi POST/PUT/DELETE request
2. Controller nhận request, validate dữ liệu
3. Tạo Command object tương ứng
4. Gửi Command qua `CommandGateway.sendAndWait()`
5. Axon routing Command đến Aggregate phù hợp
6. Aggregate xử lý và publish Event
7. Trả về response cho client

### Event Objects

Events là các sự kiện đã xảy ra (past tense), immutable.

#### MovieCreateEvent.java

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieCreateEvent {
    private String id;
    private String title;
    private String description;
    private Integer duration;
    private String posterUrl;
}
```

**Đặc điểm:**

- Tên event ở thì quá khứ: `Created`, `Updated`, `Deleted`
- Chứa toàn bộ dữ liệu cần thiết để rebuild state
- Immutable (không thể thay đổi sau khi tạo)

---

## Query Side (Đọc Dữ Liệu)

### Projection Pattern

**Projection** lắng nghe Events từ Command side và cập nhật Read Database.

#### MovieProjection.java

```java
@Component
public class MovieProjection {

    private final MovieRepository movieRepository;

    public MovieProjection(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // Event Handler - Lắng nghe events từ Command side
    @EventHandler
    public void on(MovieCreateEvent event) {
        Movie movie = Movie.builder()
            .id(event.getId())
            .title(event.getTitle())
            .description(event.getDescription())
            .duration(event.getDuration())
            .posterUrl(event.getPosterUrl())
            .build();

        movieRepository.save(movie);
    }

    @EventHandler
    public void on(MovieUpdatedEvent event) {
        Movie movie = movieRepository.findById(event.getId())
            .orElseThrow(() -> new RuntimeException("Movie not found"));

        movie.setTitle(event.getTitle());
        movie.setDescription(event.getDescription());
        movie.setDuration(event.getDuration());
        movie.setPosterUrl(event.getPosterUrl());

        movieRepository.save(movie);
    }

    @EventHandler
    public void on(MovieDeletedEvent event) {
        movieRepository.deleteById(event.getId());
    }

    // Query Handler - Xử lý queries từ client
    @QueryHandler
    public List<MovieResponseModel> handle(GetAllMoviesQuery query) {
        List<Movie> movies = movieRepository.findAll();
        return movies.stream()
            .map(movie -> {
                MovieResponseModel model = new MovieResponseModel();
                BeanUtils.copyProperties(movie, model);
                return model;
            })
            .collect(Collectors.toList());
    }

    @QueryHandler
    public MovieResponseModel handle(GetMovieByIdQuery query) throws Exception {
        Movie movie = movieRepository.findById(query.getId())
            .orElseThrow(() -> new Exception("Not found movie: " + query.getId()));

        MovieResponseModel model = new MovieResponseModel();
        BeanUtils.copyProperties(movie, model);
        return model;
    }
}
```

**Giải thích:**

- `@EventHandler`: Lắng nghe và xử lý events
- `@QueryHandler`: Xử lý queries từ client
- Repository pattern để tương tác với database

### Query Objects

Queries là các yêu cầu đọc dữ liệu.

#### GetAllMoviesQuery.java

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAllMoviesQuery {
    // Có thể chứa filter parameters
}
```

#### GetMovieByIdQuery.java

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetMovieByIdQuery {
    private String id;
}
```

### Query Controllers

Controllers xử lý HTTP GET requests.

#### MovieQueryController.java

```java
@RestController
@RequestMapping("/api/v1/movies")
public class MovieQueryController {

    private final QueryGateway queryGateway;

    public MovieQueryController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    // GET ALL
    @GetMapping
    public List<MovieResponseModel> getAllMovies() {
        GetAllMoviesQuery query = new GetAllMoviesQuery();
        return queryGateway
            .query(query, ResponseTypes.multipleInstancesOf(MovieResponseModel.class))
            .join();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public MovieResponseModel getMovieById(@PathVariable String id) {
        GetMovieByIdQuery query = new GetMovieByIdQuery(id);
        return queryGateway
            .query(query, ResponseTypes.instanceOf(MovieResponseModel.class))
            .join();
    }
}
```

**Giải thích:**

- `.join()`: Block và chờ kết quả từ QueryHandler
- `ResponseTypes.multipleInstancesOf()`: Cho List results
- `ResponseTypes.instanceOf()`: Cho single result

---

## API Endpoints

### 🎬 Movie APIs

#### 1. Tạo Movie Mới

```http
POST /api/v1/movies
Content-Type: application/json

{
  "title": "Avatar 3",
  "description": "The return of Na'vi",
  "duration": 180,
  "posterUrl": "https://example.com/avatar3.jpg"
}

Response (201 Created):
{
  "success": true,
  "message": "Tạo phim thành công",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

#### 2. Lấy Danh Sách Movies

````http
GET /api/v1/movies

Response (200 OK):
{
### 👥 Employee APIs

#### 1. Tạo Employee Mới

```http
POST /api/v1/employees
Content-Type: application/json

{
  "userId": "user-uuid-123",
  "cinemaId": "cinema-uuid-456",
  "position": "STAFF",
  "status": "ACTIVE"
}

Response (201 Created):
{
  "success": true,
  "message": "Tạo nhân viên thành công",
  "data": {
    "id": "emp-uuid-789"
  }
}
````

#### 2. Lấy Danh Sách Employees

```http
GET /api/v1/employees

Response (200 OK):
{
  "success": true,
  "data": [
    {
      "id": "emp-uuid-789",
      "userId": "user-uuid-123",
      "cinemaId": "cinema-uuid-456",
      "position": "STAFF",
      "status": "ACTIVE",
      "joinedAt": "2026-01-14T08:00:00"
    }
  ]
}
```

#### 3. Lấy Employee Theo ID

```http
GET /api/v1/employees/{id}
```

#### 4. Lấy Employees Theo Cinema

```http
GET /api/v1/employees/cinema/{cinemaId}
```

#### 5. Cập Nhật Employee

```http
PUT /api/v1/employees/{id}
Content-Type: application/json

{
  "userId": "user-uuid-123",
  "cinemaId": "cinema-uuid-456",
  "position": "MANAGER",
  "status": "ACTIVE"
}
```

#### 6. Xóa Employee

```http
DELETE /api/v1/employees/{id}
```

### 📅 WorkShift APIs

#### 1. Tạo WorkShift Mới

```http
POST /api/v1/workshifts
Content-Type: application/json

{
  "employeeId": "emp-uuid-789",
  "shiftName": "Sáng",
  "startTime": "2026-01-15T08:00:00",
  "endTime": "2026-01-15T12:00:00",
  "isAttended": false
}

Response (201 Created):
{
  "success": true,
  "message": "Tạo ca làm việc thành công",
  "data": {
    "id": "shift-uuid-999"
  }
}
```

#### 2. Lấy Danh Sách WorkShifts

```http
GET /api/v1/workshifts

Response (200 OK):
{
  "success": true,
  "data": [
    {
      "id": "shift-uuid-999",
      "employeeId": "emp-uuid-789",
      "shiftName": "Sáng",
      "startTime": "2026-01-15T08:00:00",
      "endTime": "2026-01-15T12:00:00",
      "isAttended": false
    }
  ]
}
```

#### 3. Lấy WorkShift Theo ID

```http
GET /api/v1/workshifts/{id}
```

#### 4. Lấy WorkShifts Theo Employee

```http
GET /api/v1/workshifts/employee/{employeeId}
```

#### 5. Cập Nhật WorkShift (Điểm danh)

```http
PUT /api/v1/workshifts/{id}
Content-Type: application/json

{
  "employeeId": "emp-uuid-789",
  "shiftName": "Sáng",
  "startTime": "2026-01-15T08:00:00",
  "endTime": "2026-01-15T12:00:00",
  "isAttended": true
}
```

#### 6. Xóa WorkShift

```http
DELETE /api/v1/workshifts/{id}
```

"success": true,
"data": [
{
"id": "123e4567-e89b-12d3-a456-426614174000",
"title": "Avatar 3",
"description": "The return of Na'vi",
"duration": 180,
"posterUrl": "https://example.com/avatar3.jpg",
"createdAt": "2026-01-13T10:30:00"
}
]
}

````

#### 3. Lấy Movie Theo ID

```http
GET /api/v1/movies/{id}

Response (200 OK):
{
  "success": true,
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "title": "Avatar 3",
    "description": "The return of Na'vi",
    "duration": 180,
    "posterUrl": "https://example.com/avatar3.jpg",
    "createdAt": "2026-01-13T10:30:00"
  }
}
````

#### 4. Cập Nhật Movie

```http
PUT /api/v1/movies/{id}
Content-Type: application/json

{
  "title": "Avatar 3: The Way of Water",
  "description": "Updated description",
  "duration": 195,
  "posterUrl": "https://example.com/avatar3-v2.jpg"
}

Response (200 OK):
{
  "success": true,
  "message": "Cập nhật phim thành công",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

#### 5. Xóa Movie

```http
DELETE /api/v1/movies/{id}

Response (200 OK):
{
  "success": true,
  "message": "Xóa phim thành công",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

### 🏢 Cinema APIs

Tương tự Movies, có đầy đủ CRUD operations:

- `POST /api/v1/cinemas` - Tạo rạp mới
- `GET /api/v1/cinemas` - Lấy danh sách rạp
- `GET /api/v1/cinemas/{id}` - Lấy rạp theo ID
- `PUT /api/v1/cinemas/{id}` - Cập nhật rạp
- `DELETE /api/v1/cinemas/{id}` - Xóa rạp

### 💺 Seat APIs

- `POST /api/v1/seats` - Tạo ghế mới
- `GET /api/v1/seats` - Lấy danh sách ghế
- `GET /api/v1/seats/{id}` - Lấy ghế theo ID
- `GET /api/v1/seats/cinema/{cinemaId}` - Lấy ghế theo rạp
- `PUT /api/v1/seats/{id}` - Cập nhật ghế
- `DELETE /api/v1/seats/{id}` - Xóa ghế

### 🎞️ ShowTime APIs

- `POST /api/v1/showtimes` - Tạo suất chiếu
- `GET /api/v1/showtimes` - Lấy danh sách suất chiếu
- `GET /api/v1/showtimes/{id}` - Lấy suất chiếu theo ID
- `GET /api/v1/showtimes/movie/{movieId}` - Lấy suất chiếu theo phim
- `PUT /api/v1/showtimes/{id}` - Cập nhật suất chiếu
- `DELETE /api/v1/showtimes/{id}` - Xóa suất chiếu

---

## Cấu Hình

### application.properties

```properties
# Application Info
spring.application.name=movies
server.port=9001

# Eureka Client - Service Discovery
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# H2 Database Configuration
spring.datasource.url=jdbc:h2:mem:moviesDB
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update

# H2 Console (Development only)
spring.h2.console.enabled=true
# Access: http://localhost:9001/h2-console

# Axon Server (Optional - nếu dùng Axon Server)
# axon.axonserver.servers=axonserver:8124
```

### Giải thích cấu hình:

| Property                                | Giá trị                      | Ý nghĩa                   |
| --------------------------------------- | ---------------------------- | ------------------------- |
| `server.port`                           | 9001                         | Port của Movies service   |
| `eureka.client.service-url.defaultZone` | http://localhost:8761/eureka | URL của Eureka Server     |
| `spring.datasource.url`                 | jdbc:h2:mem:moviesDB         | In-memory H2 database     |
| `spring.jpa.hibernate.ddl-auto`         | update                       | Tự động tạo/update schema |
| `spring.h2.console.enabled`             | true                         | Bật H2 web console        |

---

## Hướng Dẫn Cài Đặt

### Prerequisites

- ☕ **Java 17+** đã được cài đặt
- 📦 **Maven 3.6+** đã được cài đặt
- 🌐 **Eureka Server** đang chạy trên port 8761
- (Optional) **Axon Server** nếu muốn dùng distributed event store

### Bước 1: Clone Repository

CRUD operations cho Employees

- ✅ CRUD operations cho WorkShifts
- ✅

```bash
git clone <repository-url>
cd movies
```

### Bước 2: Build Project

```bash
# Windows
.\mvnw.cmd clean package -DskipTests

# Linux/Mac
./mvnw clean package -DskipTests
```

### Bước 3: Chạy Application

#### Option 1: Maven

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

#### Option 2: JAR file

```bash
java -jar target/movies-0.0.1-SNAPSHOT.jar
```

#### Option 3: IDE (IntelliJ/Eclipse)

- Mở project trong IDE
- Run `MoviesApplication.java`

### Bước 4: Verify Service

1. **Check Service Health:**

```bash
curl http://localhost:9001/actuator/health
```

2. **Check Eureka Registration:**

- Mở browser: http://localhost:8761
- Xem service "MOVIES" đã đăng ký chưa

3. **Test API:**

```bash
curl http://localhost:9001/api/v1/movies
```

4. **Access H2 Console:**

- URL: http://localhost:9001/h2-console
- JDBC URL: `jdbc:h2:mem:moviesDB`
- Username: `sa`
- Password: (để trống)

---

## Testing

### 1. Unit Testing

#### Test Aggregate

```java
@Test
public void testCreateMovieCommand() {
    // Given
    String id = UUID.randomUUID().toString();
    CreateMovieCommand command = new CreateMovieCommand(
        id, "Test Movie", "Description", 120, "poster.jpg"
    );

    // When & Then
    fixture.givenNoPriorActivity()
           .when(command)
           .expectEvents(new MovieCreateEvent(
               id, "Test Movie", "Description", 120, "poster.jpg"
           ));
}
```

#### Test Controller

```java
@WebMvcTest(MovieCommandController.class)
public class MovieCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommandGateway commandGateway;

    @Test
    public void testCreateMovie() throws Exception {
        // Given
        MovieRequestModel request = new MovieRequestModel(
            "Test Movie", "Description", 120, "poster.jpg"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/movies")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").exists());
    }
}
```

### 2. Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
public class MovieIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testCreateAndRetrieveMovie() {
        // Create
        MovieRequestModel request = new MovieRequestModel(...);
        ResponseEntity<CommandResponse> createResponse =
            restTemplate.postForEntity("/api/v1/movies", request, CommandResponse.class);

        String movieId = createResponse.getBody().getId();

        // Retrieve
        ResponseEntity<MovieResponseModel> getResponse =
            restTemplate.getForEntity("/api/v1/movies/" + movieId, MovieResponseModel.class);

        assertEquals("Test Movie", getResponse.getBody().getTitle());
    }
}
```

### 3. Manual Testing với Postman

Import file: `Cinema-Microservices-API.postman_collection.json`

Collection bao gồm:

- ✅ CRUD operations cho Movies
- ✅ CRUD operations cho Cinemas
- ✅ CRUD operations cho Seats
- ✅ CRUD operations cho ShowTimes
- ✅ Environment variables

---

## Xử Lý Replay Event Fail

### Vấn Đề: Event Replay Failures

Trong Event Sourcing, khi một Command Handler thất bại và throw exception, event đã được publish có thể gây ra **event replay failures** - tức là khi aggregate được rebuild từ event store, nó có thể gặp lỗi tương tự.

#### Nguyên nhân phổ biến:

1. **Validation Logic trong EventSourcingHandler**

   ```java
   // ❌ KHÔNG NÊN - Validation trong EventSourcingHandler
   @EventSourcingHandler
   public void on(MovieCreateEvent event) {
       if (event.getTitle() == null) {
           throw new IllegalArgumentException("Title cannot be null");
       }
       this.title = event.getTitle();
   }
   ```

2. **External Dependencies trong Aggregate**

   ```java
   // ❌ KHÔNG NÊN - Gọi external service trong EventSourcingHandler
   @EventSourcingHandler
   public void on(MovieCreateEvent event) {
       externalService.notifyCreation(event); // Fail khi replay!
   }
   ```

3. **Thiếu State Validation**
   ```java
   // ❌ KHÔNG NÊN - Không check state trước khi update
   @CommandHandler
   public void handle(UpdateMovieCommand command) {
       // Nếu aggregate chưa tồn tại, event sẽ fail khi replay
       AggregateLifecycle.apply(new MovieUpdatedEvent(...));
   }
   ```

### ✅ Giải Pháp 1: Validation trong Command Handler

**Nguyên tắc:** Validate TẤT CẢ business rules TRƯỚC KHI publish event.

```java
@Aggregate
@NoArgsConstructor
@Slf4j
public class MovieAggregate {

    @AggregateIdentifier
    private String id;
    private String title;
    private String description;
    private Integer duration;
    private String posterUrl;

    // ✅ Validation trong Command Handler
    @CommandHandler
    public MovieAggregate(CreateMovieCommand command) {
        log.info("CreateMovieCommand received - ID: {}, Title: {}",
                 command.getId(), command.getTitle());

        // Validate business rules
        if (command.getId() == null || command.getTitle() == null) {
            throw new IllegalArgumentException("Movie id and title must not be null");
        }

        if (command.getDuration() != null && command.getDuration() < 1) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        // Chỉ publish event KHI validation pass
        MovieCreateEvent event = new MovieCreateEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

    // ✅ EventSourcingHandler đơn giản, không validation
    @EventSourcingHandler
    public void on(MovieCreateEvent event) {
        // Chỉ cập nhật state, KHÔNG validation
        this.id = event.getId();
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.duration = event.getDuration();
        this.posterUrl = event.getPosterUrl();
    }
}
```

**Lợi ích:**

- Event chỉ được publish khi data đã valid
- EventSourcingHandler luôn thành công khi replay
- Business logic tập trung ở Command Handler

### ✅ Giải Pháp 2: State Validation cho Update/Delete

```java
@Aggregate
@NoArgsConstructor
@Slf4j
public class MovieAggregate {

    // UPDATE MOVIE
    @CommandHandler
    public void handle(UpdateMovieCommand command) {
        log.info("UpdateMovieCommand received - ID: {}", command.getId());

        // ✅ Check aggregate đã tồn tại chưa
        if (this.id == null) {
            throw new IllegalStateException("Movie does not exist");
        }

        // ✅ Validate dữ liệu update
        if (command.getTitle() == null || command.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        MovieUpdatedEvent event = new MovieUpdatedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

    // DELETE MOVIE
    @CommandHandler
    public void handle(DeleteMovieCommand command) {
        log.info("DeleteMovieCommand received - ID: {}", command.getId());

        // ✅ Check aggregate tồn tại
        if (this.id == null) {
            throw new IllegalStateException("Movie does not exist");
        }

        MovieDeletedEvent event = new MovieDeletedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
        AggregateLifecycle.markDeleted(); // ⭐ Đánh dấu aggregate đã xóa
    }

    @EventSourcingHandler
    public void on(MovieDeletedEvent event) {
        // ✅ Không cần set field gì
        // Aggregate đã được markDeleted
    }
}
```

**Lợi ích:**

- Ngăn update/delete aggregate không tồn tại
- `markDeleted()` ngăn commands mới đến aggregate đã xóa
- Event replay luôn consistent

### ✅ Giải Pháp 3: Exception Handling ở Controller

```java
@RestController
@RequestMapping("/api/v1/movies")
@Slf4j
public class MovieCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private MovieRepository movieRepository;

    @PostMapping
    @ApiMessage("Tạo phim thành công")
    public CommandResponse createMovie(@Valid @RequestBody MovieRequestModel model) {
        String id = UUID.randomUUID().toString();

        log.info("Creating movie - Title: {}, Duration: {}",
                 model.getTitle(), model.getDuration());

        CreateMovieCommand command = new CreateMovieCommand(
                id,
                model.getTitle(),
                model.getDescription(),
                model.getDuration(),
                model.getPosterUrl());

        try {
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to create movie: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo phim: " + e.getMessage());
        }

        return new CommandResponse(id);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa phim thành công")
    public CommandResponse deleteMovie(@PathVariable String id) {

        // ✅ Cleanup resources trước khi xóa
        try {
            Movie movie = movieRepository.findById(id).orElse(null);
            if (movie != null && movie.getPosterUrl() != null) {
                minioService.deleteFileByUrl(movie.getPosterUrl());
                log.info("Deleted poster for movie: {}", id);
            }
        } catch (Exception e) {
            log.warn("Failed to delete poster: {}", e.getMessage());
            // Không throw - tiếp tục xóa movie
        }

        try {
            DeleteMovieCommand command = new DeleteMovieCommand(id);
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to delete movie: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể xóa phim: " + e.getMessage());
        }

        return new CommandResponse(id);
    }
}
```

**Lợi ích:**

- Catch exceptions từ Command Handler
- Cleanup external resources (files, cache, etc.)
- Trả về error message rõ ràng cho client

### ✅ Giải Pháp 4: Idempotent Event Handlers

```java
@Component
public class MovieProjection {

    private final MovieRepository movieRepository;

    @EventHandler
    public void on(MovieCreateEvent event) {
        // ✅ Idempotent - check tồn tại trước khi create
        if (movieRepository.existsById(event.getId())) {
            log.warn("Movie already exists: {}", event.getId());
            return; // Skip duplicate event
        }

        Movie movie = new Movie();
        BeanUtils.copyProperties(event, movie);
        movieRepository.save(movie);
    }

    @EventHandler
    public void on(MovieUpdatedEvent event) {
        // ✅ Safe - sử dụng Optional
        Movie movie = movieRepository.findById(event.getId())
            .orElseThrow(() -> new RuntimeException("Movie not found: " + event.getId()));

        movie.setTitle(event.getTitle());
        movie.setDescription(event.getDescription());
        movie.setDuration(event.getDuration());
        movie.setPosterUrl(event.getPosterUrl());

        movieRepository.save(movie);
    }

    @EventHandler
    public void on(MovieDeletedEvent event) {
        // ✅ Idempotent - không throw nếu không tồn tại
        if (movieRepository.existsById(event.getId())) {
            movieRepository.deleteById(event.getId());
        } else {
            log.warn("Movie already deleted: {}", event.getId());
        }
    }
}
```

**Lợi ích:**

- Xử lý duplicate events an toàn
- Không crash khi replay events
- Đảm bảo eventual consistency

### ⚠️ Anti-Patterns CẦN TRÁNH

#### ❌ KHÔNG dùng @DisallowReply

```java
// ❌ TRÁNH - Che giấu lỗi thay vì fix root cause
@CommandHandler
@DisallowReply  // Không nên dùng
public MovieAggregate(CreateMovieCommand command) {
    // Exception ở đây sẽ không được báo về client
    AggregateLifecycle.apply(new MovieCreateEvent(...));
}
```

**Tại sao không nên:**

- Che giấu lỗi thật sự
- Client không biết command fail
- Khó debug khi có vấn đề
- Event vẫn bị lỗi khi replay

#### ❌ KHÔNG catch Exception trong EventSourcingHandler

```java
// ❌ TRÁNH - Nuốt exception trong EventSourcingHandler
@EventSourcingHandler
public void on(MovieCreateEvent event) {
    try {
        this.title = event.getTitle();
        // ... other code
    } catch (Exception e) {
        log.error("Error: ", e);
        // Event replay sẽ skip lỗi này!
    }
}
```

**Tại sao không nên:**

- State của aggregate sẽ không đúng
- Aggregate bị corrupt
- Khó phát hiện vấn đề

### 📋 Checklist: Tránh Event Replay Failures

Khi viết Command Handler, đảm bảo:

- [ ] **Validate đầy đủ** trong CommandHandler TRƯỚC KHI publish event
- [ ] **Check state** (aggregate đã tồn tại chưa) cho Update/Delete
- [ ] **Không có logic phức tạp** trong EventSourcingHandler
- [ ] **Không gọi external services** trong Aggregate
- [ ] **Use `markDeleted()`** khi delete aggregate
- [ ] **Idempotent Event Handlers** trong Projection
- [ ] **Try-catch ở Controller** để handle exceptions
- [ ] **Log đầy đủ** để dễ debug

### 🎯 Summary: Event Replay Best Practices

| Concern                | Command Side                  | Event Side         | Query Side             |
| ---------------------- | ----------------------------- | ------------------ | ---------------------- |
| **Validation**         | ✅ Validate tất cả            | ❌ Không validate  | ✅ Optional validation |
| **State Check**        | ✅ Bắt buộc cho Update/Delete | ❌ Không check     | ✅ Check existence     |
| **Exception Handling** | ✅ Try-catch ở Controller     | ❌ Không catch     | ✅ Handle gracefully   |
| **External Calls**     | ⚠️ Chỉ ở Controller           | ❌ Tuyệt đối không | ✅ Được phép           |
| **Idempotency**        | ⚠️ UUID prevents duplicate    | ✅ Bắt buộc        | ✅ Bắt buộc            |

---

## Best Practices

### 1. Command Side

✅ **DO:**

- Validate dữ liệu ở Controller trước khi tạo Command
- Sử dụng UUID cho aggregate IDs
- Log tất cả commands quan trọng
- Xử lý exceptions trong Aggregate
- Giữ Aggregates nhỏ gọn, tập trung vào business logic

---

## Tổng Kết Entities

Movies Service hiện quản lý **6 entities chính**:

| Entity        | Mô tả               | Quan hệ                           |
| ------------- | ------------------- | --------------------------------- |
| **Movie**     | Thông tin phim      | 1-N với ShowTime                  |
| **Cinema**    | Thông tin rạp chiếu | 1-N với Seat, ShowTime, Employee  |
| **Seat**      | Ghế ngồi trong rạp  | N-1 với Cinema                    |
| **ShowTime**  | Suất chiếu phim     | N-1 với Movie, Cinema             |
| **Employee**  | Nhân viên rạp       | N-1 với Cinema, 1-N với WorkShift |
| **WorkShift** | Ca làm việc         | N-1 với Employee                  |

### Luồng Nghiệp Vụ Chính

1. **Quản lý Phim**: Tạo/Cập nhật/Xóa phim với poster
2. **Quản lý Rạp**: Tạo rạp và ghế ngồi
3. **Lập Lịch Chiếu**: Tạo suất chiếu cho phim tại rạp cụ thể
4. **Quản lý Nhân Sự**: Thêm nhân viên vào rạp
5. **Quản lý Ca Làm**: Xếp ca và điểm danh nhân viên

---

**Version:** 1.1.0  
**Last Updated:** 2026-01-14

- Không query database trong Aggregate
- Không gọi external services trong CommandHandler
- Không throw exceptions trong EventSourcingHandler

### 2. Query Side

✅ **DO:**

- Tạo dedicated ResponseModels cho queries
- Sử dụng Projections để cập nhật Read DB
- Implement caching cho queries thường xuyên
- Paginate large result sets

❌ **DON'T:**

- Không update database trực tiếp, chỉ qua Events
- Không query Event Store từ Query side

### 3. Event Handling

✅ **DO:**

- Đặt tên Events ở thì quá khứ (Created, Updated, Deleted)
- Events phải immutable
- Chứa đủ thông tin để rebuild state
- Implement idempotency (xử lý duplicate events)

❌ **DON'T:**

- Không thay đổi structure của Events đã publish (versioning)
- Không skip events trong EventSourcingHandler

### 4. Error Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CommandExecutionException.class)
    public ResponseEntity<ErrorResponse> handleCommandException(
            CommandExecutionException ex) {

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

### 5. Monitoring & Logging

```java
@Slf4j
@Aggregate
public class MovieAggregate {

    @CommandHandler
    public MovieAggregate(CreateMovieCommand command) {
        log.info("Creating movie: id={}, title={}",
                 command.getId(), command.getTitle());
        // ...
    }

    @EventSourcingHandler
    public void on(MovieCreateEvent event) {
        log.debug("Movie created event applied: id={}", event.getId());
        // ...
    }
}
```

---

## Troubleshooting

### Vấn đề 1: Service không đăng ký với Eureka

**Triệu chứng:**

- Service chạy nhưng không xuất hiện trong Eureka Dashboard

**Giải pháp:**

```yaml
# Kiểm tra application.properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# Kiểm tra @EnableDiscoveryClient trong MoviesApplication.java
@SpringBootApplication
@EnableDiscoveryClient  // ← Đảm bảo có annotation này
public class MoviesApplication { }
```

### Vấn đề 2: Command không được xử lý

**Triệu chứng:**

- POST request thành công nhưng không có dữ liệu trong DB

**Giải pháp:**

1. Check logs cho CommandHandler execution
2. Verify Event được publish
3. Check EventHandler trong Projection có được gọi không

```bash
# Enable debug logging
logging.level.org.axonframework=DEBUG
```

### Vấn đề 3: H2 Database bị reset

**Triệu chứng:**

- Dữ liệu mất sau khi restart

**Giải thích:**

- H2 in-memory database sẽ reset khi restart
- Đây là behavior bình thường trong development

**Giải pháp (nếu muốn persist):**

```properties
# Đổi sang file-based H2
spring.datasource.url=jdbc:h2:file:./data/moviesDB
```

---

## Kết Luận

Movies Service là một microservice được thiết kế theo **CQRS** và **Event Sourcing** patterns, cung cấp:

✨ **Ưu điểm:**

- **Scalability**: Query side và Command side có thể scale độc lập
- **Performance**: Read và Write được tối ưu riêng biệt
- **Audit Trail**: Toàn bộ changes được lưu trong Event Store
- **Flexibility**: Dễ dàng thêm Projections mới mà không ảnh hưởng Command side
- **Maintainability**: Code rõ ràng, tách biệt concerns

⚠️ **Trade-offs:**

- **Eventual Consistency**: Query side có thể chậm hơn Command side một chút
- **Complexity**: Phức tạp hơn traditional CRUD
- **Learning Curve**: Cần hiểu rõ CQRS/ES concepts

📚 **Tài liệu tham khảo:**

- [Axon Framework Documentation](https://docs.axoniq.io/reference-guide/)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

---

**Version:** 1.0.0  
**Last Updated:** 2026-01-13  
**Author:** Cinema Development Team  
**Contact:** support@cinema.com
