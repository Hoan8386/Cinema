package com.cinema.movies.query.projection;

import com.cinema.movies.command.data.ShowTime;
import com.cinema.movies.command.data.Reponsitory.ShowTimeRepository;
import com.cinema.movies.query.model.ShowTimeResponseModel;
import com.cinema.movies.query.queries.GetAllShowTimesQuery;
import com.cinema.movies.query.queries.GetShowTimeByIdQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ShowTimeProjection {

    private final ShowTimeRepository showTimeRepository;

    public ShowTimeProjection(ShowTimeRepository showTimeRepository) {
        this.showTimeRepository = showTimeRepository;
    }

    @QueryHandler
    public List<ShowTimeResponseModel> handle(GetAllShowTimesQuery query) {
        List<ShowTime> showTimes = showTimeRepository.findAll();
        return showTimes.stream()
                .map(this::mapToResponseModel)
                .collect(Collectors.toList());
    }

    @QueryHandler
    public ShowTimeResponseModel handle(GetShowTimeByIdQuery query) {
        ShowTime showTime = showTimeRepository.findById(query.getId()).orElse(null);
        if (showTime == null) {
            return null;
        }
        return mapToResponseModel(showTime);
    }

    private ShowTimeResponseModel mapToResponseModel(ShowTime showTime) {
        ShowTimeResponseModel model = new ShowTimeResponseModel();
        model.setId(showTime.getId());
        model.setStartTime(showTime.getStartTime());
        model.setPrice(showTime.getPrice());
        if (showTime.getMovie() != null) {
            model.setMovieId(showTime.getMovie().getId());
            model.setMovieTitle(showTime.getMovie().getTitle());
        }
        if (showTime.getCinema() != null) {
            model.setCinemaId(showTime.getCinema().getId());
            model.setCinemaName(showTime.getCinema().getName());
        }
        return model;
    }
}
