package com.cinema.movies.command.data.Reponsitory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cinema.movies.command.data.Cinema;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, String> {
}
