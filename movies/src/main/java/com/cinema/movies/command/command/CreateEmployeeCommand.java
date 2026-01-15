package com.cinema.movies.command.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEmployeeCommand {

    @TargetAggregateIdentifier
    private String id;

    private String userId;
    private String cinemaId;
    private String position;
    private String status;
}
