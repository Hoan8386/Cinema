package com.cinema.movies.command.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.cinema.commonservice.annotation.ApiMessage;
import com.cinema.movies.command.command.CreateMovieCommand;
import com.cinema.movies.command.command.UpdateMovieCommand;
import com.cinema.movies.command.command.DeleteMovieCommand;
import com.cinema.movies.command.model.CommandResponse;
import com.cinema.movies.command.model.MovieRequestModel;

@RestController
@RequestMapping("/api/v1/movies")
@Slf4j
public class MovieCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    @ApiMessage("Tạo phim thành công")
    public CommandResponse createMovie(@Valid @RequestBody MovieRequestModel model) {

        String id = UUID.randomUUID().toString();

        log.info("Received request - Title: {}, Description: {}, Duration: {}",
                model.getTitle(), model.getDescription(), model.getDuration());

        CreateMovieCommand command = new CreateMovieCommand(
                id,
                model.getTitle(),
                model.getDescription(),
                model.getDuration(),
                model.getPosterUrl());

        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật phim thành công")
    public CommandResponse updateMovie(
            @PathVariable String id,
            @Valid @RequestBody MovieRequestModel model) {

        UpdateMovieCommand command = new UpdateMovieCommand(
                id,
                model.getTitle(),
                model.getDescription(),
                model.getDuration(),
                model.getPosterUrl());

        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa phim thành công")
    public CommandResponse deleteMovie(@PathVariable String id) {

        DeleteMovieCommand command = new DeleteMovieCommand(id);
        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }
}
