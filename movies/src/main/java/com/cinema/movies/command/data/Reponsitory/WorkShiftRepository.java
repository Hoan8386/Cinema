package com.cinema.movies.command.data.Reponsitory;

import com.cinema.movies.command.data.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkShiftRepository extends JpaRepository<WorkShift, String> {

    List<WorkShift> findByEmployeeId(String employeeId);

    List<WorkShift> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<WorkShift> findByEmployeeIdAndIsAttended(String employeeId, Boolean isAttended);
}
