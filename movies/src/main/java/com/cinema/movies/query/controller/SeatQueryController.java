package com.cinema.movies.query.controller;

import com.cinema.movies.query.model.SeatResponseModel;
import com.cinema.movies.query.queries.GetAllSeatsQuery;
import com.cinema.movies.query.queries.GetSeatByIdQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seats")
public class SeatQueryController {

    private final QueryGateway queryGateway;

    public SeatQueryController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping
    public List<SeatResponseModel> getAllSeats() {
        GetAllSeatsQuery query = new GetAllSeatsQuery();
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(SeatResponseModel.class)).join();
    }

    @GetMapping("/{id}")
    public SeatResponseModel getSeatById(@PathVariable String id) {
        GetSeatByIdQuery query = new GetSeatByIdQuery(id);
        return queryGateway.query(query, ResponseTypes.instanceOf(SeatResponseModel.class)).join();
    }
}
