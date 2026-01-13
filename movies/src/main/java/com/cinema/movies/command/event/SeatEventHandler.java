package com.cinema.movies.command.event;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void on(SeatCreatedEvent event) {
        Seat seat = new Seat();
        seat.setId(event.getId());
        seat.setCinema(cinemaRepository.getReferenceById(event.getCinemaId()));
        seat.setSeatRow(event.getSeatRow());
        seat.setSeatNumber(event.getSeatNumber());
        seatRepository.save(seat);
    }

    @EventHandler
    @Transactional
    public void on(SeatUpdatedEvent event) {
        Seat seat = seatRepository.findById(event.getId()).orElse(null);
        if (seat != null) {
            seat.setCinema(cinemaRepository.getReferenceById(event.getCinemaId()));
            seat.setSeatRow(event.getSeatRow());
            seat.setSeatNumber(event.getSeatNumber());
            seatRepository.save(seat);
        }
    }

    @EventHandler
    @Transactional
    public void on(SeatDeletedEvent event) {
        seatRepository.deleteById(event.getId());
    }
}
