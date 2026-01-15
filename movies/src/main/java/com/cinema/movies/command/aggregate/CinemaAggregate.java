package com.cinema.movies.command.aggregate;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import com.cinema.movies.command.command.CreateCinemaCommand;
import com.cinema.movies.command.command.UpdateCinemaCommand;
import com.cinema.movies.command.command.DeleteCinemaCommand;
import com.cinema.movies.command.event.CinemaCreatedEvent;
import com.cinema.movies.command.event.CinemaUpdatedEvent;
import com.cinema.movies.command.event.CinemaDeletedEvent;

@Aggregate
@NoArgsConstructor
@Slf4j
public class CinemaAggregate {

    /*
     * =======================
     * AGGREGATE STATE
     * =======================
     */

    @AggregateIdentifier
    private String id;

    private String name;
    private String address;

    /*
     * =======================
     * COMMAND HANDLERS
     * =======================
     */

    // CREATE CINEMA
    @CommandHandler
    public CinemaAggregate(CreateCinemaCommand command) {

        log.info("CreateCinemaCommand received - ID: {}, Name: {}",
                command.getId(), command.getName());

        // Validate business rule
        if (command.getId() == null || command.getName() == null) {
            throw new IllegalArgumentException("Cinema id and name must not be null");
        }

        CinemaCreatedEvent event = new CinemaCreatedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // UPDATE CINEMA
    @CommandHandler
    public void handle(UpdateCinemaCommand command) {

        log.info("UpdateCinemaCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("Cinema does not exist");
        }

        CinemaUpdatedEvent event = new CinemaUpdatedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // DELETE CINEMA
    @CommandHandler
    public void handle(DeleteCinemaCommand command) {

        log.info("DeleteCinemaCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("Cinema does not exist");
        }

        CinemaDeletedEvent event = new CinemaDeletedEvent();
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
    public void on(CinemaCreatedEvent event) {
        this.id = event.getId();
        this.name = event.getName();
        this.address = event.getAddress();
    }

    @EventSourcingHandler
    public void on(CinemaUpdatedEvent event) {
        this.name = event.getName();
        this.address = event.getAddress();
    }

    @EventSourcingHandler
    public void on(CinemaDeletedEvent event) {
        // Không cần set field nào
        // Aggregate đã bị markDeleted
    }
}
