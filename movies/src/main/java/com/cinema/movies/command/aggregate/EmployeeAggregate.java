package com.cinema.movies.command.aggregate;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import com.cinema.movies.command.command.CreateEmployeeCommand;
import com.cinema.movies.command.command.UpdateEmployeeCommand;
import com.cinema.movies.command.command.DeleteEmployeeCommand;
import com.cinema.movies.command.event.EmployeeCreatedEvent;
import com.cinema.movies.command.event.EmployeeUpdatedEvent;
import com.cinema.movies.command.event.EmployeeDeletedEvent;

@Aggregate
@NoArgsConstructor
@Slf4j
public class EmployeeAggregate {

    /*
     * =======================
     * AGGREGATE STATE
     * =======================
     */

    @AggregateIdentifier
    private String id;

    private String userId;
    private String cinemaId;
    private String position;
    private String status;

    /*
     * =======================
     * COMMAND HANDLERS
     * =======================
     */

    // CREATE EMPLOYEE
    @CommandHandler
    public EmployeeAggregate(CreateEmployeeCommand command) {

        log.info("CreateEmployeeCommand received - ID: {}, UserID: {}, CinemaID: {}",
                command.getId(), command.getUserId(), command.getCinemaId());

        // Validate business rule
        if (command.getId() == null || command.getUserId() == null || command.getCinemaId() == null) {
            throw new IllegalArgumentException("Employee id, userId and cinemaId must not be null");
        }

        EmployeeCreatedEvent event = new EmployeeCreatedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // UPDATE EMPLOYEE
    @CommandHandler
    public void handle(UpdateEmployeeCommand command) {

        log.info("UpdateEmployeeCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("Employee does not exist");
        }

        EmployeeUpdatedEvent event = new EmployeeUpdatedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // DELETE EMPLOYEE
    @CommandHandler
    public void handle(DeleteEmployeeCommand command) {

        log.info("DeleteEmployeeCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("Employee does not exist");
        }

        EmployeeDeletedEvent event = new EmployeeDeletedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
        AggregateLifecycle.markDeleted(); // ⭐ RẤT QUAN TRỌNG
    }

    /*
     * =======================
     * EVENT SOURCING HANDLERS
     * =======================
     */

    @EventSourcingHandler
    public void on(EmployeeCreatedEvent event) {
        this.id = event.getId();
        this.userId = event.getUserId();
        this.cinemaId = event.getCinemaId();
        this.position = event.getPosition();
        this.status = event.getStatus();
    }

    @EventSourcingHandler
    public void on(EmployeeUpdatedEvent event) {
        this.userId = event.getUserId();
        this.cinemaId = event.getCinemaId();
        this.position = event.getPosition();
        this.status = event.getStatus();
    }

    @EventSourcingHandler
    public void on(EmployeeDeletedEvent event) {
        // Không cần set field nào
        // Aggregate đã bị markDeleted
    }
}
