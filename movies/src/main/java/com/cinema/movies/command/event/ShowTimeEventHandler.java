package com.cinema.movies.command.event;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cinema.movies.command.data.Movie;
import com.cinema.movies.command.data.Cinema;
import com.cinema.movies.command.data.ShowTime;
import com.cinema.movies.command.data.Reponsitory.MovieRepository;
import com.cinema.movies.command.data.Reponsitory.CinemaRepository;
import com.cinema.movies.command.data.Reponsitory.ShowTimeRepository;

@Component
public class ShowTimeEventHandler {

    @Autowired
    private ShowTimeRepository showTimeRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @EventHandler
    public void on(ShowTimeCreatedEvent event) {
        Movie movie = movieRepository.findById(event.getMovieId()).orElse(null);
        Cinema cinema = cinemaRepository.findById(event.getCinemaId()).orElse(null);

        if (movie != null && cinema != null) {
            ShowTime showTime = new ShowTime();
            showTime.setId(event.getId());
            showTime.setMovie(movie);
            showTime.setCinema(cinema);
            showTime.setStartTime(event.getStartTime());
            showTime.setPrice(event.getPrice());
            showTimeRepository.save(showTime);
        }
    }

    @EventHandler
    public void on(ShowTimeUpdatedEvent event) {
        ShowTime showTime = showTimeRepository.findById(event.getId()).orElse(null);
        if (showTime != null) {
            Movie movie = movieRepository.findById(event.getMovieId()).orElse(null);
            Cinema cinema = cinemaRepository.findById(event.getCinemaId()).orElse(null);

            if (movie != null && cinema != null) {
                showTime.setMovie(movie);
                showTime.setCinema(cinema);
                showTime.setStartTime(event.getStartTime());
                showTime.setPrice(event.getPrice());
                showTimeRepository.save(showTime);
            }
        }
    }

    @EventHandler
    public void on(ShowTimeDeletedEvent event) {
        showTimeRepository.deleteById(event.getId());
    }
}
