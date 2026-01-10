package com.cinema.movies.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatResponseModel {
    private String id;
    private String cinemaId;
    private String cinemaName;
    private String seatRow;
    private Integer seatNumber;
}
