package com.cinema.movies.command.aggregate;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Aggregate
@NoArgsConstructor
@Slf4j
public class ShowTimeAggregate {

    /*
     * =======================
     * AGGREGATE STATE
     * =======================
     */

    @AggregateIdentifier
    private String id;

    private String movieId;
    private String cinemaId;
    private LocalDateTime startTime;
    private BigDecimal price;

    /*
     * =======================
     * COMMAND HANDLERS
     * =======================
     */

    // CREATE SHOWTIME
    @CommandHandler
    public ShowTimeAggregate(CreateShowTimeCommand command) {

        log.info("CreateShowTimeCommand received - ID: {}, MovieID: {}, CinemaID: {}",
                command.getId(), command.getMovieId(), command.getCinemaId());

        // Validate business rule
        if (command.getId() == null || command.getMovieId() == null || command.getCinemaId() == null) {
            throw new IllegalArgumentException("ShowTime id, movieId and cinemaId must not be null");
        }

        ShowTimeCreatedEvent event = new ShowTimeCreatedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // UPDATE SHOWTIME
    @CommandHandler
    public void handle(UpdateShowTimeCommand command) {

        log.info("UpdateShowTimeCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("ShowTime does not exist");
        }

        ShowTimeUpdatedEvent event = new ShowTimeUpdatedEvent();
        BeanUtils.copyProperties(command, event);

        AggregateLifecycle.apply(event);
    }

    // DELETE SHOWTIME
    @CommandHandler
    public void handle(DeleteShowTimeCommand command) {

        log.info("DeleteShowTimeCommand received - ID: {}", command.getId());

        // Aggregate must exist
        if (this.id == null) {
            throw new IllegalStateException("ShowTime does not exist");
        }

        ShowTimeDeletedEvent event = new ShowTimeDeletedEvent();
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
        // Không cần set field nào
        // Aggregate đã bị markDeleted
    }
}
