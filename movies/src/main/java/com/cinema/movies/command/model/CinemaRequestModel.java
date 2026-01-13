package com.cinema.movies.command.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CinemaRequestModel {

    @NotBlank(message = "Tên rạp không được để trống")
    @Size(min = 2, max = 100, message = "Tên rạp phải có độ dài từ 2 đến 100 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(min = 5, max = 255, message = "Địa chỉ phải có độ dài từ 5 đến 255 ký tự")
    private String address;
}
