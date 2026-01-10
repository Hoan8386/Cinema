package com.cinema.movies.command.aggregate;

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

import lombok.NoArgsConstructor;

@Aggregate
@NoArgsConstructor
public class SeatAggregate {

    @AggregateIdentifier
    private String id;
    private String cinemaId;
    private String seatRow;
    private Integer seatNumber;

    @CommandHandler
    public SeatAggregate(CreateSeatCommand command) {
        SeatCreatedEvent event = new SeatCreatedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

    @CommandHandler
    public void handle(UpdateSeatCommand command) {
        SeatUpdatedEvent event = new SeatUpdatedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

    @CommandHandler
    public void handle(DeleteSeatCommand command) {
        SeatDeletedEvent event = new SeatDeletedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

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
        this.id = event.getId();
    }
}
