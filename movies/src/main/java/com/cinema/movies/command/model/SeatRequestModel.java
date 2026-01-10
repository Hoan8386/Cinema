package com.cinema.movies.command.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SeatRequestModel {
    private String cinemaId;
    private String seatRow;
    private Integer seatNumber;
}
