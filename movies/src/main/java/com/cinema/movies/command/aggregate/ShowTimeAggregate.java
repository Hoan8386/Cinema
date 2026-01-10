package com.cinema.movies.command.aggregate;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import com.cinema.movies.command.command.CreateShowTimeCommand;
import com.cinema.movies.command.command.UpdateShowTimeCommand;
import com.cinema.movies.command.command.DeleteShowTimeCommand;
import com.cinema.movies.command.event.ShowTimeCreatedEvent;
import com.cinema.movies.command.event.ShowTimeUpdatedEvent;
import com.cinema.movies.command.event.ShowTimeDeletedEvent;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Aggregate
@NoArgsConstructor
public class ShowTimeAggregate {

    @AggregateIdentifier
    private String id;
    private String movieId;
    private String cinemaId;
    private LocalDateTime startTime;
    private BigDecimal price;

    @CommandHandler
    public ShowTimeAggregate(CreateShowTimeCommand command) {
        ShowTimeCreatedEvent event = new ShowTimeCreatedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

    @CommandHandler
    public void handle(UpdateShowTimeCommand command) {
        ShowTimeUpdatedEvent event = new ShowTimeUpdatedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

    @CommandHandler
    public void handle(DeleteShowTimeCommand command) {
        ShowTimeDeletedEvent event = new ShowTimeDeletedEvent();
        BeanUtils.copyProperties(command, event);
        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(ShowTimeCreatedEvent event) {
        this.id = event.getId();
        this.movieId = event.getMovieId();
        this.cinemaId = event.getCinemaId();
        this.startTime = event.getStartTime();
        this.price = event.getPrice();
    }

    @EventSourcingHandler
    public void on(ShowTimeUpdatedEvent event) {
        this.movieId = event.getMovieId();
        this.cinemaId = event.getCinemaId();
        this.startTime = event.getStartTime();
        this.price = event.getPrice();
    }

    @EventSourcingHandler
    public void on(ShowTimeDeletedEvent event) {
        this.id = event.getId();
    }
}
