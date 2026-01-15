package com.cinema.movies.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeCreatedEvent {
    private String id;
    private String userId;
    private String cinemaId;
    private String position;
    private String status;
}
