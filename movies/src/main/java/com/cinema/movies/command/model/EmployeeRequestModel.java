package com.cinema.movies.command.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeRequestModel {

    @NotBlank(message = "UserId không được để trống")
    private String userId;

    @NotBlank(message = "CinemaId không được để trống")
    private String cinemaId;

    private String position; // MANAGER, STAFF, TICKET_SELLER

    private String status; // ACTIVE, RESIGNED
}
