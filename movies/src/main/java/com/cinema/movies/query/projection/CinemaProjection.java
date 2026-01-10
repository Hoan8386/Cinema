package com.cinema.movies.query.projection;

import com.cinema.movies.command.data.Cinema;
import com.cinema.movies.command.data.Reponsitory.CinemaRepository;
import com.cinema.movies.query.model.CinemaResponseModel;
import com.cinema.movies.query.queries.GetAllCinemasQuery;
import com.cinema.movies.query.queries.GetCinemaByIdQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CinemaProjection {

    private final CinemaRepository cinemaRepository;

    public CinemaProjection(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    @QueryHandler
    public List<CinemaResponseModel> handle(GetAllCinemasQuery query) {
        List<Cinema> cinemas = cinemaRepository.findAll();
        return cinemas.stream()
                .map(cinema -> {
                    CinemaResponseModel model = new CinemaResponseModel();
                    BeanUtils.copyProperties(cinema, model);
                    return model;
                })
                .collect(Collectors.toList());
    }

    @QueryHandler
    public CinemaResponseModel handle(GetCinemaByIdQuery query) {
        Cinema cinema = cinemaRepository.findById(query.getId()).orElse(null);
        if (cinema == null) {
            return null;
        }
        CinemaResponseModel model = new CinemaResponseModel();
        BeanUtils.copyProperties(cinema, model);
        return model;
    }
}
