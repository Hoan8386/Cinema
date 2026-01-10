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
public class UpdateSeatCommand {

    @TargetAggregateIdentifier
    private String id;

    private String cinemaId;
    private String seatRow;
    private Integer seatNumber;
}
