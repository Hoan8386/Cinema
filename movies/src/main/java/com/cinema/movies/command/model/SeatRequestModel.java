package com.cinema.movies.command.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SeatRequestModel {

    @NotBlank(message = "ID rạp chiếu không được để trống")
    private String cinemaId;

    @NotBlank(message = "Hàng ghế không được để trống")
    @Pattern(regexp = "^[A-Z]$", message = "Hàng ghế phải là chữ cái in hoa (A-Z)")
    private String seatRow;

    @NotNull(message = "Số ghế không được để trống")
    @Min(value = 1, message = "Số ghế phải lớn hơn 0")
    @Max(value = 100, message = "Số ghế không được vượt quá 100")
    private Integer seatNumber;
}
