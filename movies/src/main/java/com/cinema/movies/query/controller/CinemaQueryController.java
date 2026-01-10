package com.cinema.movies.query.controller;

import com.cinema.movies.query.model.CinemaResponseModel;
import com.cinema.movies.query.queries.GetAllCinemasQuery;
import com.cinema.movies.query.queries.GetCinemaByIdQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cinemas")
public class CinemaQueryController {

    private final QueryGateway queryGateway;

    public CinemaQueryController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping
    public List<CinemaResponseModel> getAllCinemas() {
        GetAllCinemasQuery query = new GetAllCinemasQuery();
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(CinemaResponseModel.class)).join();
    }

    @GetMapping("/{id}")
    public CinemaResponseModel getCinemaById(@PathVariable String id) {
        GetCinemaByIdQuery query = new GetCinemaByIdQuery(id);
        return queryGateway.query(query, ResponseTypes.instanceOf(CinemaResponseModel.class)).join();
    }
}
