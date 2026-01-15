package com.cinema.movies.command.controller;

import java.util.UUID;

import com.cinema.movies.command.data.Employee;
import com.cinema.movies.command.data.Reponsitory.EmployeeRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.cinema.commonservice.annotation.ApiMessage;
import com.cinema.movies.command.command.CreateEmployeeCommand;
import com.cinema.movies.command.command.UpdateEmployeeCommand;
import com.cinema.movies.command.command.DeleteEmployeeCommand;
import com.cinema.movies.command.model.CommandResponse;
import com.cinema.movies.command.model.EmployeeRequestModel;

@RestController
@RequestMapping("/api/v1/employees")
@Slf4j
public class EmployeeCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private EmployeeRepository employeeRepository;

    @PostMapping
    @ApiMessage("Tạo nhân viên thành công")
    public CommandResponse createEmployee(@Valid @RequestBody EmployeeRequestModel model) {

        String id = UUID.randomUUID().toString();

        log.info("Creating employee - UserID: {}, CinemaID: {}, Position: {}",
                model.getUserId(), model.getCinemaId(), model.getPosition());

        CreateEmployeeCommand command = new CreateEmployeeCommand(
                id,
                model.getUserId(),
                model.getCinemaId(),
                model.getPosition(),
                model.getStatus());

        try {
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to create employee: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo nhân viên: " + e.getMessage());
        }

        return new CommandResponse(id);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật nhân viên thành công")
    public CommandResponse updateEmployee(
            @PathVariable String id,
            @Valid @RequestBody EmployeeRequestModel model) {

        log.info("Updating employee - ID: {}", id);

        UpdateEmployeeCommand command = new UpdateEmployeeCommand(
                id,
                model.getUserId(),
                model.getCinemaId(),
                model.getPosition(),
                model.getStatus());

        try {
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to update employee: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể cập nhật nhân viên: " + e.getMessage());
        }

        return new CommandResponse(id);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa nhân viên thành công")
    public CommandResponse deleteEmployee(@PathVariable String id) {

        log.info("Deleting employee - ID: {}", id);

        try {
            DeleteEmployeeCommand command = new DeleteEmployeeCommand(id);
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to delete employee: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể xóa nhân viên: " + e.getMessage());
        }

        return new CommandResponse(id);
    }
}
