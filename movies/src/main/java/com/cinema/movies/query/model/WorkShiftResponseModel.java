package com.cinema.movies.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkShiftResponseModel {
    private String id;
    private String employeeId;
    private String shiftName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isAttended;
}
