package com.cinema.movies.command.aggregate;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Aggregate
@NoArgsConstructor
@Slf4j
public class MovieAggregate {

    /*
     * =======================
     * AGGREGATE STATE
     * =======================
     */

    @AggregateIdentifier
    private String id;

    private String title;
    private String description;
    private Integer duration;
    private String posterUrl;

    /*
     * =======================
     * COMMAND HANDLERS
     * =======================
     */

    // CREATE MOVIE
    @CommandHandler
    public MovieAggregate(CreateMovieCommand command) {

        log.info("CreateMovieCommand received - ID: {}, Title: {}",
                command.getId(), command.getTitle());

        // Validate business rule
        if (command.getId() == null || command.getTitle() == null) {
            throw new IllegalArgumentException("Movie id and title must not be null");
        }

        MovieCreateEvent event = new MovieCreateEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // UPDATE MOVIE
    @CommandHandler
    public void handle(UpdateMovieCommand command) {

        log.info("UpdateMovieCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("Movie does not exist");
        }

        MovieUpdatedEvent event = new MovieUpdatedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // DELETE MOVIE
    @CommandHandler
    public void handle(DeleteMovieCommand command) {

        log.info("DeleteMovieCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("Movie does not exist");
        }

        MovieDeletedEvent event = new MovieDeletedEvent();
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
    public void on(MovieCreateEvent event) {
        this.id = event.getId();
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.duration = event.getDuration();
        this.posterUrl = event.getPosterUrl();
    }

    @EventSourcingHandler
    public void on(MovieUpdatedEvent event) {
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.duration = event.getDuration();
        this.posterUrl = event.getPosterUrl();
    }

    @EventSourcingHandler
    public void on(MovieDeletedEvent event) {
        // Không cần set field nào
        // Aggregate đã bị markDeleted
    }
}
