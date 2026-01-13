package com.cinema.movies.query.projection;

import com.cinema.movies.command.data.Movie;
import com.cinema.movies.command.data.Reponsitory.MovieRepository;
import com.cinema.movies.query.model.MovieResponseModel;
import com.cinema.movies.query.queries.GetAllMoviesQuery;
import com.cinema.movies.query.queries.GetMovieByIdQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MovieProjection {

    private final MovieRepository movieRepository;

    public MovieProjection(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @QueryHandler
    public List<MovieResponseModel> handle(GetAllMoviesQuery query) {
        List<Movie> movies = movieRepository.findAll();
        return movies.stream()
                .map(movie -> {
                    MovieResponseModel model = new MovieResponseModel();
                    BeanUtils.copyProperties(movie, model);
                    return model;
                })
                .collect(Collectors.toList());
    }

    @QueryHandler
    public MovieResponseModel handle(GetMovieByIdQuery query) throws Exception {
        Movie movie = movieRepository.findById(query.getId())
                .orElseThrow(() -> new Exception("Not found movies :" + query.getId()));
        MovieResponseModel model = new MovieResponseModel();
        BeanUtils.copyProperties(movie, model);
        return model;
    }
}
