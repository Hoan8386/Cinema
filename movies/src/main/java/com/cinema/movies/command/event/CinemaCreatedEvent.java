package com.cinema.movies.command.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CinemaCreatedEvent {
    private String id;
    private String name;
    private String address;
}
