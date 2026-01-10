package com.cinema.movies.command.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShowTimeRequestModel {
    private String movieId;
    private String cinemaId;
    private LocalDateTime startTime;
    private BigDecimal price;
}
