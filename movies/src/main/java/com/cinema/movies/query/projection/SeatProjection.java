package com.cinema.movies.query.projection;

import com.cinema.movies.command.data.Seat;
import com.cinema.movies.command.data.Reponsitory.SeatRepository;
import com.cinema.movies.query.model.SeatResponseModel;
import com.cinema.movies.query.queries.GetAllSeatsQuery;
import com.cinema.movies.query.queries.GetSeatByIdQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SeatProjection {

    private final SeatRepository seatRepository;

    public SeatProjection(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @QueryHandler
    @Transactional(readOnly = true)
    public List<SeatResponseModel> handle(GetAllSeatsQuery query) {
        List<Seat> seats = seatRepository.findAllWithDetails();
        return seats.stream()
                .map(this::mapToResponseModel)
                .collect(Collectors.toList());
    }

    @QueryHandler
    @Transactional(readOnly = true)
    public SeatResponseModel handle(GetSeatByIdQuery query) throws Exception {
        Seat seat = seatRepository.findByIdWithDetails(query.getId())
                .orElseThrow(() -> new Exception("Not found seats :" + query.getId()));
        return mapToResponseModel(seat);
    }

    private SeatResponseModel mapToResponseModel(Seat seat) {
        SeatResponseModel model = new SeatResponseModel();
        model.setId(seat.getId());
        model.setSeatRow(seat.getSeatRow());
        model.setSeatNumber(seat.getSeatNumber());
        if (seat.getCinema() != null) {
            model.setCinemaId(seat.getCinema().getId());
            model.setCinemaName(seat.getCinema().getName());
        }
        return model;
    }
}
