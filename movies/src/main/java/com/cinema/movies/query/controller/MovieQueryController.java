package com.cinema.movies.query.controller;

import com.cinema.movies.query.model.MovieResponseModel;
import com.cinema.movies.query.queries.GetAllMoviesQuery;
import com.cinema.movies.query.queries.GetMovieByIdQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieQueryController {

    private final QueryGateway queryGateway;

    public MovieQueryController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping
    public List<MovieResponseModel> getAllMovies() {
        GetAllMoviesQuery query = new GetAllMoviesQuery();
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(MovieResponseModel.class)).join();
    }

    @GetMapping("/{id}")
    public MovieResponseModel getMovieById(@PathVariable String id) {
        GetMovieByIdQuery query = new GetMovieByIdQuery(id);
        return queryGateway.query(query, ResponseTypes.instanceOf(MovieResponseModel.class)).join();
    }
}
