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
import com.cinema.commonservice.service.KafkaService;
import com.cinema.movies.command.command.CreateCinemaCommand;
import com.cinema.movies.command.command.UpdateCinemaCommand;
import com.cinema.movies.command.command.DeleteCinemaCommand;
import com.cinema.movies.command.model.CommandResponse;
import com.cinema.movies.command.model.CinemaRequestModel;

@RestController
@RequestMapping("/api/v1/cinemas")
@Slf4j
public class CinemaCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private KafkaService kafkaService;

    @PostMapping
    @ApiMessage("Tạo rạp chiếu phim thành công")
    public CommandResponse createCinema(@Valid @RequestBody CinemaRequestModel model) {
        String id = UUID.randomUUID().toString();

        log.info("Received request - Name: {}, Address: {}",
                model.getName(), model.getAddress());

        CreateCinemaCommand command = new CreateCinemaCommand(
                id,
                model.getName(),
                model.getAddress());

        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật rạp chiếu phim thành công")
    public CommandResponse updateCinema(@PathVariable String id, @Valid @RequestBody CinemaRequestModel model) {
        UpdateCinemaCommand command = new UpdateCinemaCommand(
                id,
                model.getName(),
                model.getAddress());

        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa rạp chiếu phim thành công")
    public CommandResponse deleteCinema(@PathVariable String id) {
        DeleteCinemaCommand command = new DeleteCinemaCommand(id);
        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }

    @PostMapping("/sendMessage")
    public void sendMessage(@RequestBody String message) {
        kafkaService.sendMessage("cinema", message);
    }

}
