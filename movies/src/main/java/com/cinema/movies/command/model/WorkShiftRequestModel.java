package com.cinema.movies.command.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkShiftRequestModel {

    @NotNull(message = "EmployeeId không được để trống")
    private String employeeId;

    private String shiftName; // Sáng, Chiều, Tối

    @NotNull(message = "StartTime không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "EndTime không được để trống")
    private LocalDateTime endTime;

    private Boolean isAttended;
}
