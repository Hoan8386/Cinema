package com.cinema.movies.command.event;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cinema.movies.command.data.Cinema;
import com.cinema.movies.command.data.Reponsitory.CinemaRepository;

@Component
public class CinemaEventHandler {

    @Autowired
    private CinemaRepository cinemaRepository;

    @EventHandler
    public void on(CinemaCreatedEvent event) {
        Cinema cinema = new Cinema();
        cinema.setId(event.getId());
        cinema.setName(event.getName());
        cinema.setAddress(event.getAddress());
        cinemaRepository.save(cinema);
    }

    @EventHandler
    public void on(CinemaUpdatedEvent event) {
        Cinema cinema = cinemaRepository.findById(event.getId()).orElse(null);
        if (cinema != null) {
            cinema.setName(event.getName());
            cinema.setAddress(event.getAddress());
            cinemaRepository.save(cinema);
        }
    }

    @EventHandler
    public void on(CinemaDeletedEvent event) {
        cinemaRepository.deleteById(event.getId());
    }
}
