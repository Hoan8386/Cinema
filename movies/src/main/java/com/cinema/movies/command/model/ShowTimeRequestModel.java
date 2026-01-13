package com.cinema.movies.command.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShowTimeRequestModel {

    @NotBlank(message = "ID phim không được để trống")
    private String movieId;

    @NotBlank(message = "ID rạp chiếu không được để trống")
    private String cinemaId;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @Future(message = "Thời gian bắt đầu phải là thời điểm trong tương lai")
    private LocalDateTime startTime;

    @NotNull(message = "Giá vé không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá vé phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá vé không hợp lệ")
    private BigDecimal price;
}
