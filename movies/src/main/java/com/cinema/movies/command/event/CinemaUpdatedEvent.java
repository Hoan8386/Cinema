package com.cinema.movies.command.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CinemaUpdatedEvent {
    private String id;
    private String name;
    private String address;
}
