package com.cinema.movies.command.data.Reponsitory;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.movies.command.data.Movie;

public interface MovieRepository extends JpaRepository<Movie, String> {

}
