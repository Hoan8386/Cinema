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

import com.cinema.movies.command.command.CreateShowTimeCommand;
import com.cinema.movies.command.command.UpdateShowTimeCommand;
import com.cinema.movies.command.command.DeleteShowTimeCommand;
import com.cinema.movies.command.model.ShowTimeRequestModel;

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowTimeCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    public String createShowTime(@Valid @RequestBody ShowTimeRequestModel model) {
        CreateShowTimeCommand command = new CreateShowTimeCommand(
                UUID.randomUUID().toString(),
                model.getMovieId(),
                model.getCinemaId(),
                model.getStartTime(),
                model.getPrice());
        return commandGateway.sendAndWait(command);
    }

    @PutMapping("/{id}")
    public String updateShowTime(@PathVariable String id, @Valid @RequestBody ShowTimeRequestModel model) {
        UpdateShowTimeCommand command = new UpdateShowTimeCommand(
                id,
                model.getMovieId(),
                model.getCinemaId(),
                model.getStartTime(),
                model.getPrice());
        commandGateway.sendAndWait(command);
        return "ShowTime updated successfully";
    }

    @DeleteMapping("/{id}")
    public String deleteShowTime(@PathVariable String id) {
        DeleteShowTimeCommand command = new DeleteShowTimeCommand(id);
        commandGateway.sendAndWait(command);
        return "ShowTime deleted successfully";
    }
}
