package com.cinema.movies.command.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateMovieCommand {

    @TargetAggregateIdentifier
    private String id;

    private String title;

    private String description;

    private Integer duration; // Thời lượng tính bằng phút

    private String posterUrl;
}
