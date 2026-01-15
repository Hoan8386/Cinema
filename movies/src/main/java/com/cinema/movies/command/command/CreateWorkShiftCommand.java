package com.cinema.movies.command.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateWorkShiftCommand {

    @TargetAggregateIdentifier
    private String id;

    private String employeeId;
    private String shiftName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isAttended;
}
