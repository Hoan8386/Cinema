package com.cinema.movies.command.controller;

import java.util.UUID;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.movies.command.command.CreateCinemaCommand;
import com.cinema.movies.command.command.UpdateCinemaCommand;
import com.cinema.movies.command.command.DeleteCinemaCommand;
import com.cinema.movies.command.model.CinemaRequestModel;

@RestController
@RequestMapping("/api/v1/cinemas")
public class CinemaCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    public String createCinema(@RequestBody CinemaRequestModel model) {
        CreateCinemaCommand command = new CreateCinemaCommand(
                UUID.randomUUID().toString(),
                model.getName(),
                model.getAddress());
        return commandGateway.sendAndWait(command);
    }

    @PutMapping("/{id}")
    public String updateCinema(@PathVariable String id, @RequestBody CinemaRequestModel model) {
        UpdateCinemaCommand command = new UpdateCinemaCommand(
                id,
                model.getName(),
                model.getAddress());
        commandGateway.sendAndWait(command);
        return "Cinema updated successfully";
    }

    @DeleteMapping("/{id}")
    public String deleteCinema(@PathVariable String id) {
        DeleteCinemaCommand command = new DeleteCinemaCommand(id);
        commandGateway.sendAndWait(command);
        return "Cinema deleted successfully";
    }
}
