package com.cinema.movies.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeResponseModel {
    private String id;
    private String userId;
    private String cinemaId;
    private String position;
    private String status;
    private LocalDateTime joinedAt;
}
