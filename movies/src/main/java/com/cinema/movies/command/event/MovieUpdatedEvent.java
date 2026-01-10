package com.cinema.movies.command.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MovieUpdatedEvent {
    private String id;

    private String title;

    private String description;

    private Integer duration; // Thời lượng tính bằng phút

    private String posterUrl;
}
