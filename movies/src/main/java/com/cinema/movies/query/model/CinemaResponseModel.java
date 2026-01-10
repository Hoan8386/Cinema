package com.cinema.movies.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CinemaResponseModel {
    private String id;
    private String name;
    private String address;
}
