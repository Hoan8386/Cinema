package com.cinema.movies.command.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MovieRequestModel {

    private Long id;

    @NotBlank(message = "Tiêu đề phim không được để trống")
    @Size(min = 1, max = 200, message = "Tiêu đề phim phải có độ dài từ 1 đến 200 ký tự")
    private String title;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    @NotNull(message = "Thời lượng phim không được để trống")
    @Min(value = 1, message = "Thời lượng phim phải lớn hơn 0 phút")
    @Max(value = 500, message = "Thời lượng phim không được vượt quá 500 phút")
    private Integer duration; // Thời lượng tính bằng phút

    @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$", message = "URL poster không hợp lệ", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String posterUrl;

}
