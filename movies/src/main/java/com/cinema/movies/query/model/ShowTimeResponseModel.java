package com.cinema.movies.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShowTimeResponseModel {
    private String id;
    private String movieId;
    private String movieTitle;
    private String cinemaId;
    private String cinemaName;
    private LocalDateTime startTime;
    private BigDecimal price;
}
