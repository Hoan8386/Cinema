package com.cinema.movies.command.event;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cinema.movies.command.data.Cinema;
import com.cinema.movies.command.data.Seat;
import com.cinema.movies.command.data.Reponsitory.CinemaRepository;
import com.cinema.movies.command.data.Reponsitory.SeatRepository;

@Component
public class SeatEventHandler {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @EventHandler
    public void on(SeatCreatedEvent event) {
        Cinema cinema = cinemaRepository.findById(event.getCinemaId()).orElse(null);
        if (cinema != null) {
            Seat seat = new Seat();
            seat.setId(event.getId());
            seat.setCinema(cinema);
            seat.setSeatRow(event.getSeatRow());
            seat.setSeatNumber(event.getSeatNumber());
            seatRepository.save(seat);
        }
    }

    @EventHandler
    public void on(SeatUpdatedEvent event) {
        Seat seat = seatRepository.findById(event.getId()).orElse(null);
        if (seat != null) {
            Cinema cinema = cinemaRepository.findById(event.getCinemaId()).orElse(null);
            if (cinema != null) {
                seat.setCinema(cinema);
                seat.setSeatRow(event.getSeatRow());
                seat.setSeatNumber(event.getSeatNumber());
                seatRepository.save(seat);
            }
        }
    }

    @EventHandler
    public void on(SeatDeletedEvent event) {
        seatRepository.deleteById(event.getId());
    }
}
