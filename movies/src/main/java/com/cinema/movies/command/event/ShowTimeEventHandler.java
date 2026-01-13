package com.cinema.movies.command.event;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void on(ShowTimeCreatedEvent event) {
        ShowTime showTime = new ShowTime();
        showTime.setId(event.getId());
        showTime.setMovie(movieRepository.getReferenceById(event.getMovieId()));
        showTime.setCinema(cinemaRepository.getReferenceById(event.getCinemaId()));
        showTime.setStartTime(event.getStartTime());
        showTime.setPrice(event.getPrice());
        showTimeRepository.save(showTime);
    }

    @EventHandler
    @Transactional
    public void on(ShowTimeUpdatedEvent event) {
        ShowTime showTime = showTimeRepository.findById(event.getId()).orElse(null);
        if (showTime != null) {
            showTime.setMovie(movieRepository.getReferenceById(event.getMovieId()));
            showTime.setCinema(cinemaRepository.getReferenceById(event.getCinemaId()));
            showTime.setStartTime(event.getStartTime());
            showTime.setPrice(event.getPrice());
            showTimeRepository.save(showTime);
        }
    }

    @EventHandler
    @Transactional
    public void on(ShowTimeDeletedEvent event) {
        showTimeRepository.deleteById(event.getId());
    }
}
