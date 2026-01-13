package com.cinema.movies.command.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.movies.command.command.CreateSeatCommand;
import com.cinema.movies.command.command.UpdateSeatCommand;
import com.cinema.movies.command.command.DeleteSeatCommand;
import com.cinema.movies.command.model.SeatRequestModel;

@RestController
@RequestMapping("/api/v1/seats")
public class SeatCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    public String createSeat(@Valid @RequestBody SeatRequestModel model) {
        CreateSeatCommand command = new CreateSeatCommand(
                UUID.randomUUID().toString(),
                model.getCinemaId(),
                model.getSeatRow(),
                model.getSeatNumber());
        return commandGateway.sendAndWait(command);
    }

    @PutMapping("/{id}")
    public String updateSeat(@PathVariable String id, @Valid @RequestBody SeatRequestModel model) {
        UpdateSeatCommand command = new UpdateSeatCommand(
                id,
                model.getCinemaId(),
                model.getSeatRow(),
                model.getSeatNumber());
        commandGateway.sendAndWait(command);
        return "Seat updated successfully";
    }

    @DeleteMapping("/{id}")
    public String deleteSeat(@PathVariable String id) {
        DeleteSeatCommand command = new DeleteSeatCommand(id);
        commandGateway.sendAndWait(command);
        return "Seat deleted successfully";
    }
}
