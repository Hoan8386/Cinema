package com.cinema.movies.command.aggregate;

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

import lombok.NoArgsConstructor;

@Aggregate
@NoArgsConstructor
public class CinemaAggregate {

    @AggregateIdentifier
    private String id;
    private String name;
    private String address;

    @CommandHandler
    public CinemaAggregate(CreateCinemaCommand command) {
        CinemaCreatedEvent event = new CinemaCreatedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

    @CommandHandler
    public void handle(UpdateCinemaCommand command) {
        CinemaUpdatedEvent event = new CinemaUpdatedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

    @CommandHandler
    public void handle(DeleteCinemaCommand command) {
        CinemaDeletedEvent event = new CinemaDeletedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

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
        this.id = event.getId();
    }
}
