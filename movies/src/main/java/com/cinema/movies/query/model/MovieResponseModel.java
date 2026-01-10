package com.cinema.movies.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieResponseModel {
    private String id;
    private String title;
    private String description;
    private Integer duration;
    private String posterUrl;
}
