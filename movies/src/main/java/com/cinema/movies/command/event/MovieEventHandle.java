package com.cinema.movies.command.event;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cinema.movies.command.data.Movie;
import com.cinema.movies.command.data.Reponsitory.MovieRepository;

@Component
public class MovieEventHandle {
    @Autowired
    private MovieRepository movieRepository;

    @EventHandler
    public void on(MovieCreateEvent event) {
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
