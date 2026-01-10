package com.cinema.movies.command.data.Reponsitory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cinema.movies.command.data.ShowTime;

@Repository
public interface ShowTimeRepository extends JpaRepository<ShowTime, String> {
}
