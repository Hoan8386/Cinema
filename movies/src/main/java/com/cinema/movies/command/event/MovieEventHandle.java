package com.cinema.movies.command.event;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cinema.movies.command.data.Movie;
import com.cinema.movies.command.data.Reponsitory.MovieRepository;

@Component
@Slf4j
public class MovieEventHandle {
    @Autowired
    private MovieRepository movieRepository;

    @EventHandler
    public void on(MovieCreateEvent event) {
        log.info("Event received - ID: {}, Title: {}", event.getId(), event.getTitle());
        Movie movie = new Movie();
        BeanUtils.copyProperties(event, movie);
        movieRepository.save(movie);
    }

    @EventHandler
    public void on(MovieUpdatedEvent event) {
        Movie movie = movieRepository.findById(event.getId()).orElse(null);
        if (movie != null) {
            BeanUtils.copyProperties(event, movie);
            movieRepository.save(movie);
        }
    }

    @EventHandler
    public void on(MovieDeletedEvent event) {
        movieRepository.deleteById(event.getId());
    }
}
