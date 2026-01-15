package com.cinema.movies.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkShiftCreatedEvent {
    private String id;
    private String employeeId;
    private String shiftName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isAttended;
}
