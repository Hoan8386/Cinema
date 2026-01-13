package com.cinema.movies.command.data.Reponsitory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cinema.movies.command.data.Seat;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, String> {

    @Query("SELECT s FROM Seat s LEFT JOIN FETCH s.cinema")
    List<Seat> findAllWithDetails();

    @Query("SELECT s FROM Seat s LEFT JOIN FETCH s.cinema WHERE s.id = :id")
    Optional<Seat> findByIdWithDetails(@Param("id") String id);
}
