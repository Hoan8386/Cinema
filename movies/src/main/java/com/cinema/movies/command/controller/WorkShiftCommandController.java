package com.cinema.movies.command.controller;

import java.util.UUID;

import com.cinema.movies.command.data.WorkShift;
import com.cinema.movies.command.data.Reponsitory.WorkShiftRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.cinema.commonservice.annotation.ApiMessage;
import com.cinema.movies.command.command.CreateWorkShiftCommand;
import com.cinema.movies.command.command.UpdateWorkShiftCommand;
import com.cinema.movies.command.command.DeleteWorkShiftCommand;
import com.cinema.movies.command.model.CommandResponse;
import com.cinema.movies.command.model.WorkShiftRequestModel;

@RestController
@RequestMapping("/api/v1/workshifts")
@Slf4j
public class WorkShiftCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private WorkShiftRepository workShiftRepository;

    @PostMapping
    @ApiMessage("Tạo ca làm việc thành công")
    public CommandResponse createWorkShift(@Valid @RequestBody WorkShiftRequestModel model) {

        String id = UUID.randomUUID().toString();

        log.info("Creating work shift - EmployeeID: {}, ShiftName: {}, StartTime: {}",
                model.getEmployeeId(), model.getShiftName(), model.getStartTime());

        CreateWorkShiftCommand command = new CreateWorkShiftCommand(
                id,
                model.getEmployeeId(),
                model.getShiftName(),
                model.getStartTime(),
                model.getEndTime(),
                model.getIsAttended());

        try {
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to create work shift: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo ca làm việc: " + e.getMessage());
        }

        return new CommandResponse(id);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật ca làm việc thành công")
    public CommandResponse updateWorkShift(
            @PathVariable String id,
            @Valid @RequestBody WorkShiftRequestModel model) {

        log.info("Updating work shift - ID: {}", id);

        UpdateWorkShiftCommand command = new UpdateWorkShiftCommand(
                id,
                model.getEmployeeId(),
                model.getShiftName(),
                model.getStartTime(),
                model.getEndTime(),
                model.getIsAttended());

        try {
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to update work shift: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể cập nhật ca làm việc: " + e.getMessage());
        }

        return new CommandResponse(id);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa ca làm việc thành công")
    public CommandResponse deleteWorkShift(@PathVariable String id) {

        log.info("Deleting work shift - ID: {}", id);

        try {
            DeleteWorkShiftCommand command = new DeleteWorkShiftCommand(id);
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to delete work shift: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể xóa ca làm việc: " + e.getMessage());
        }

        return new CommandResponse(id);
    }
}
