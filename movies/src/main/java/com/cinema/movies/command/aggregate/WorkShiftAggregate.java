package com.cinema.movies.command.aggregate;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import com.cinema.movies.command.command.CreateWorkShiftCommand;
import com.cinema.movies.command.command.UpdateWorkShiftCommand;
import com.cinema.movies.command.command.DeleteWorkShiftCommand;
import com.cinema.movies.command.event.WorkShiftCreatedEvent;
import com.cinema.movies.command.event.WorkShiftUpdatedEvent;
import com.cinema.movies.command.event.WorkShiftDeletedEvent;

import java.time.LocalDateTime;

@Aggregate
@NoArgsConstructor
@Slf4j
public class WorkShiftAggregate {

    /*
     * =======================
     * AGGREGATE STATE
     * =======================
     */

    @AggregateIdentifier
    private String id;

    private String employeeId;
    private String shiftName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isAttended;

    /*
     * =======================
     * COMMAND HANDLERS
     * =======================
     */

    // CREATE WORK SHIFT
    @CommandHandler
    public WorkShiftAggregate(CreateWorkShiftCommand command) {

        log.info("CreateWorkShiftCommand received - ID: {}, EmployeeID: {}, ShiftName: {}",
                command.getId(), command.getEmployeeId(), command.getShiftName());

        // Validate business rule
        if (command.getId() == null || command.getEmployeeId() == null) {
            throw new IllegalArgumentException("WorkShift id and employeeId must not be null");
        }

        if (command.getStartTime() == null || command.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time must not be null");
        }

        if (command.getEndTime().isBefore(command.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        WorkShiftCreatedEvent event = new WorkShiftCreatedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // UPDATE WORK SHIFT
    @CommandHandler
    public void handle(UpdateWorkShiftCommand command) {

        log.info("UpdateWorkShiftCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("WorkShift does not exist");
        }

        // Validate time range
        if (command.getStartTime() != null && command.getEndTime() != null &&
                command.getEndTime().isBefore(command.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        WorkShiftUpdatedEvent event = new WorkShiftUpdatedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // DELETE WORK SHIFT
    @CommandHandler
    public void handle(DeleteWorkShiftCommand command) {

        log.info("DeleteWorkShiftCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("WorkShift does not exist");
        }

        WorkShiftDeletedEvent event = new WorkShiftDeletedEvent();
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
    public void on(WorkShiftCreatedEvent event) {
        this.id = event.getId();
        this.employeeId = event.getEmployeeId();
        this.shiftName = event.getShiftName();
        this.startTime = event.getStartTime();
        this.endTime = event.getEndTime();
        this.isAttended = event.getIsAttended();
    }

    @EventSourcingHandler
    public void on(WorkShiftUpdatedEvent event) {
        this.employeeId = event.getEmployeeId();
        this.shiftName = event.getShiftName();
        this.startTime = event.getStartTime();
        this.endTime = event.getEndTime();
        this.isAttended = event.getIsAttended();
    }

    @EventSourcingHandler
    public void on(WorkShiftDeletedEvent event) {
        // Không cần set field nào
        // Aggregate đã bị markDeleted
    }
}
