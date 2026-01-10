package com.cinema.movies.command.aggregate;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import com.cinema.movies.command.command.CreateMovieCommand;
import com.cinema.movies.command.command.UpdateMovieCommand;
import com.cinema.movies.command.command.DeleteMovieCommand;
import com.cinema.movies.command.event.MovieCreateEvent;
import com.cinema.movies.command.event.MovieUpdatedEvent;
import com.cinema.movies.command.event.MovieDeletedEvent;

import lombok.NoArgsConstructor;

@Aggregate
@NoArgsConstructor
public class MovieAggregate {
    @AggregateIdentifier
    private String id;

    private String title;

    private String description;

    private Integer duration; // Thời lượng tính bằng phút

    private String posterUrl;

    // Constructor này sẽ được gọi khi tạo aggregate mới
    @CommandHandler
    public MovieAggregate(CreateMovieCommand command) {
        MovieCreateEvent movieCreateEvent = new MovieCreateEvent();
        BeanUtils.copyProperties(command, movieCreateEvent);
        AggregateLifecycle.apply(movieCreateEvent);
    }

    // Command handler cho Update Movie
    @CommandHandler
    public void handle(UpdateMovieCommand command) {
        MovieUpdatedEvent movieUpdatedEvent = new MovieUpdatedEvent();
        BeanUtils.copyProperties(command, movieUpdatedEvent);
        AggregateLifecycle.apply(movieUpdatedEvent);
    }

    // Command handler cho Delete Movie
    @CommandHandler
    public void handle(DeleteMovieCommand command) {
        MovieDeletedEvent movieDeletedEvent = new MovieDeletedEvent();
        BeanUtils.copyProperties(command, movieDeletedEvent);
        AggregateLifecycle.apply(movieDeletedEvent);
    }

    @EventSourcingHandler
    public void on(MovieCreateEvent event) {
        this.id = event.getId();
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.duration = event.getDuration();
        this.posterUrl = event.getPosterUrl();
    }

    @EventSourcingHandler
    public void on(MovieUpdatedEvent event) {
        this.id = event.getId();
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.duration = event.getDuration();
        this.posterUrl = event.getPosterUrl();
    }

    @EventSourcingHandler
    public void on(MovieDeletedEvent event) {
        this.id = event.getId();
    }
}
