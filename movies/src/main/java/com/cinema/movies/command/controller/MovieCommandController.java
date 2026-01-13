package com.cinema.movies.command.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.movies.command.command.CreateMovieCommand;
import com.cinema.movies.command.command.UpdateMovieCommand;
import com.cinema.movies.command.command.DeleteMovieCommand;
import com.cinema.movies.command.model.MovieRequestModel;

@RestController
@RequestMapping("/api/v1/movies")
@Slf4j
public class MovieCommandController {
    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    public String createMovie(@Valid @RequestBody MovieRequestModel model) {
        log.info("Received request - Title: {}, Description: {}, Duration: {}",
                model.getTitle(), model.getDescription(), model.getDuration());
        CreateMovieCommand createMovieCommand = new CreateMovieCommand(
                UUID.randomUUID().toString(),
                model.getTitle(),
                model.getDescription(),
                model.getDuration(),
                model.getPosterUrl());
        return commandGateway.sendAndWait(createMovieCommand);
    }

    @PutMapping("/{id}")
    public String updateMovie(@PathVariable String id, @Valid @RequestBody MovieRequestModel model) {
        UpdateMovieCommand updateMovieCommand = new UpdateMovieCommand(
                id,
                model.getTitle(),
                model.getDescription(),
                model.getDuration(),
                model.getPosterUrl());
        commandGateway.sendAndWait(updateMovieCommand);
        return "Movie updated successfully";
    }

    @DeleteMapping("/{id}")
    public String deleteMovie(@PathVariable String id) {
        DeleteMovieCommand deleteMovieCommand = new DeleteMovieCommand(id);
        commandGateway.sendAndWait(deleteMovieCommand);
        return "Movie deleted successfully";
    }
}
