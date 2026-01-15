package com.cinema.movies.command.aggregate;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import com.cinema.movies.command.command.CreateSeatCommand;
import com.cinema.movies.command.command.UpdateSeatCommand;
import com.cinema.movies.command.command.DeleteSeatCommand;
import com.cinema.movies.command.event.SeatCreatedEvent;
import com.cinema.movies.command.event.SeatUpdatedEvent;
import com.cinema.movies.command.event.SeatDeletedEvent;

@Aggregate
@NoArgsConstructor
@Slf4j
public class SeatAggregate {

    /*
     * =======================
     * AGGREGATE STATE
     * =======================
     */

    @AggregateIdentifier
    private String id;

    private String cinemaId;
    private String seatRow;
    private Integer seatNumber;

    /*
     * =======================
     * COMMAND HANDLERS
     * =======================
     */

    // CREATE SEAT
    @CommandHandler
    public SeatAggregate(CreateSeatCommand command) {

        log.info("CreateSeatCommand received - ID: {}, Row: {}, Number: {}",
                command.getId(), command.getSeatRow(), command.getSeatNumber());

        // Validate business rule
        if (command.getId() == null || command.getCinemaId() == null) {
            throw new IllegalArgumentException("Seat id and cinemaId must not be null");
        }

        SeatCreatedEvent event = new SeatCreatedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // UPDATE SEAT
    @CommandHandler
    public void handle(UpdateSeatCommand command) {

        log.info("UpdateSeatCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("Seat does not exist");
        }

        SeatUpdatedEvent event = new SeatUpdatedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // DELETE SEAT
    @CommandHandler
    public void handle(DeleteSeatCommand command) {

        log.info("DeleteSeatCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("Seat does not exist");
        }

        SeatDeletedEvent event = new SeatDeletedEvent();
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
    public void on(SeatCreatedEvent event) {
        this.id = event.getId();
        this.cinemaId = event.getCinemaId();
        this.seatRow = event.getSeatRow();
        this.seatNumber = event.getSeatNumber();
    }

    @EventSourcingHandler
    public void on(SeatUpdatedEvent event) {
        this.cinemaId = event.getCinemaId();
        this.seatRow = event.getSeatRow();
        this.seatNumber = event.getSeatNumber();
    }

    @EventSourcingHandler
    public void on(SeatDeletedEvent event) {
        // Không cần set field nào
        // Aggregate đã bị markDeleted
    }
}
