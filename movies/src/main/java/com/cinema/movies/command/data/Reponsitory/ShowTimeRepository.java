package com.cinema.movies.command.data.Reponsitory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cinema.movies.command.data.ShowTime;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShowTimeRepository extends JpaRepository<ShowTime, String> {

    @Query("SELECT s FROM ShowTime s LEFT JOIN FETCH s.movie LEFT JOIN FETCH s.cinema")
    List<ShowTime> findAllWithDetails();

    @Query("SELECT s FROM ShowTime s LEFT JOIN FETCH s.movie LEFT JOIN FETCH s.cinema WHERE s.id = :id")
    Optional<ShowTime> findByIdWithDetails(@Param("id") String id);
}
