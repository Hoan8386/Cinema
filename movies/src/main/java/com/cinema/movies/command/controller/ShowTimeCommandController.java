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

import com.cinema.commonservice.annotation.ApiMessage;
import com.cinema.movies.command.command.CreateShowTimeCommand;
import com.cinema.movies.command.command.UpdateShowTimeCommand;
import com.cinema.movies.command.command.DeleteShowTimeCommand;
import com.cinema.movies.command.model.CommandResponse;
import com.cinema.movies.command.model.ShowTimeRequestModel;

@RestController
@RequestMapping("/api/v1/showtimes")
@Slf4j
public class ShowTimeCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    @ApiMessage("Tạo suất chiếu thành công")
    public CommandResponse createShowTime(@Valid @RequestBody ShowTimeRequestModel model) {
        String id = UUID.randomUUID().toString();

        log.info("Received request - MovieId: {}, CinemaId: {}, StartTime: {}, Price: {}",
                model.getMovieId(), model.getCinemaId(), model.getStartTime(), model.getPrice());

        CreateShowTimeCommand command = new CreateShowTimeCommand(
                id,
                model.getMovieId(),
                model.getCinemaId(),
                model.getStartTime(),
                model.getPrice());

        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật suất chiếu thành công")
    public CommandResponse updateShowTime(@PathVariable String id, @Valid @RequestBody ShowTimeRequestModel model) {
        UpdateShowTimeCommand command = new UpdateShowTimeCommand(
                id,
                model.getMovieId(),
                model.getCinemaId(),
                model.getStartTime(),
                model.getPrice());

        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa suất chiếu thành công")
    public CommandResponse deleteShowTime(@PathVariable String id) {
        DeleteShowTimeCommand command = new DeleteShowTimeCommand(id);
        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }
}
