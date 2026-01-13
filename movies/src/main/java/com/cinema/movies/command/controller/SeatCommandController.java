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
import com.cinema.movies.command.command.CreateSeatCommand;
import com.cinema.movies.command.command.UpdateSeatCommand;
import com.cinema.movies.command.command.DeleteSeatCommand;
import com.cinema.movies.command.model.CommandResponse;
import com.cinema.movies.command.model.SeatRequestModel;

@RestController
@RequestMapping("/api/v1/seats")
@Slf4j
public class SeatCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    @ApiMessage("Tạo ghế ngồi thành công")
    public CommandResponse createSeat(@Valid @RequestBody SeatRequestModel model) {
        String id = UUID.randomUUID().toString();

        log.info("Received request - CinemaId: {}, Row: {}, Number: {}",
                model.getCinemaId(), model.getSeatRow(), model.getSeatNumber());

        CreateSeatCommand command = new CreateSeatCommand(
                id,
                model.getCinemaId(),
                model.getSeatRow(),
                model.getSeatNumber());

        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật ghế ngồi thành công")
    public CommandResponse updateSeat(@PathVariable String id, @Valid @RequestBody SeatRequestModel model) {
        UpdateSeatCommand command = new UpdateSeatCommand(
                id,
                model.getCinemaId(),
                model.getSeatRow(),
                model.getSeatNumber());

        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa ghế ngồi thành công")
    public CommandResponse deleteSeat(@PathVariable String id) {
        DeleteSeatCommand command = new DeleteSeatCommand(id);
        commandGateway.sendAndWait(command);

        return new CommandResponse(id);
    }
}
