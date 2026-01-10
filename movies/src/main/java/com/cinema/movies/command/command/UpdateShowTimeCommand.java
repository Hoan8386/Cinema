package com.cinema.movies.command.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

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
public class UpdateShowTimeCommand {

    @TargetAggregateIdentifier
    private String id;

    private String movieId;
    private String cinemaId;
    private LocalDateTime startTime;
    private BigDecimal price;
}
