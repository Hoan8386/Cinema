package com.cinema.movies.query.controller;

import com.cinema.movies.query.model.ShowTimeResponseModel;
import com.cinema.movies.query.queries.GetAllShowTimesQuery;
import com.cinema.movies.query.queries.GetShowTimeByIdQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowTimeQueryController {

    private final QueryGateway queryGateway;

    public ShowTimeQueryController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping
    public List<ShowTimeResponseModel> getAllShowTimes() {
        GetAllShowTimesQuery query = new GetAllShowTimesQuery();
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(ShowTimeResponseModel.class)).join();
    }

    @GetMapping("/{id}")
    public ShowTimeResponseModel getShowTimeById(@PathVariable String id) {
        GetShowTimeByIdQuery query = new GetShowTimeByIdQuery(id);
        return queryGateway.query(query, ResponseTypes.instanceOf(ShowTimeResponseModel.class)).join();
    }
}
