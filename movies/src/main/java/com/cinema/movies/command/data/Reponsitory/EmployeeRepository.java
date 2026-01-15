package com.cinema.movies.command.data.Reponsitory;

import com.cinema.movies.command.data.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    List<Employee> findByCinemaId(String cinemaId);

    List<Employee> findByStatus(String status);

    List<Employee> findByCinemaIdAndStatus(String cinemaId, String status);
}
