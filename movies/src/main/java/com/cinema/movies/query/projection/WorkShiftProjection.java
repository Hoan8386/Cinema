package com.cinema.movies.query.projection;

import com.cinema.movies.command.data.Employee;
import com.cinema.movies.command.data.WorkShift;
import com.cinema.movies.command.data.Reponsitory.EmployeeRepository;
import com.cinema.movies.command.data.Reponsitory.WorkShiftRepository;
import com.cinema.movies.command.event.WorkShiftCreatedEvent;
import com.cinema.movies.command.event.WorkShiftDeletedEvent;
import com.cinema.movies.command.event.WorkShiftUpdatedEvent;
import com.cinema.movies.query.model.WorkShiftResponseModel;
import com.cinema.movies.query.queries.GetAllWorkShiftsQuery;
import com.cinema.movies.query.queries.GetWorkShiftByIdQuery;
import com.cinema.movies.query.queries.GetWorkShiftsByEmployeeQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WorkShiftProjection {

    @Autowired
    private WorkShiftRepository workShiftRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    /*
     * =======================
     * EVENT HANDLERS
     * =======================
     */

    @EventHandler
    public void on(WorkShiftCreatedEvent event) {
        log.info("WorkShiftCreatedEvent received - ID: {}", event.getId());

        // Idempotent - check exists
        if (workShiftRepository.existsById(event.getId())) {
            log.warn("WorkShift already exists: {}", event.getId());
            return;
        }

        Employee employee = employeeRepository.findById(event.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found: " + event.getEmployeeId()));

        WorkShift workShift = WorkShift.builder()
                .id(event.getId())
                .employee(employee)
                .shiftName(event.getShiftName())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .isAttended(event.getIsAttended() != null ? event.getIsAttended() : false)
                .build();

        workShiftRepository.save(workShift);
    }

    @EventHandler
    public void on(WorkShiftUpdatedEvent event) {
        log.info("WorkShiftUpdatedEvent received - ID: {}", event.getId());

        WorkShift workShift = workShiftRepository.findById(event.getId())
                .orElseThrow(() -> new RuntimeException("WorkShift not found: " + event.getId()));

        Employee employee = employeeRepository.findById(event.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found: " + event.getEmployeeId()));

        workShift.setEmployee(employee);
        workShift.setShiftName(event.getShiftName());
        workShift.setStartTime(event.getStartTime());
        workShift.setEndTime(event.getEndTime());
        workShift.setIsAttended(event.getIsAttended());

        workShiftRepository.save(workShift);
    }

    @EventHandler
    public void on(WorkShiftDeletedEvent event) {
        log.info("WorkShiftDeletedEvent received - ID: {}", event.getId());

        if (workShiftRepository.existsById(event.getId())) {
            workShiftRepository.deleteById(event.getId());
        } else {
            log.warn("WorkShift already deleted: {}", event.getId());
        }
    }

    /*
     * =======================
     * QUERY HANDLERS
     * =======================
     */

    @QueryHandler
    public List<WorkShiftResponseModel> handle(GetAllWorkShiftsQuery query) {
        log.info("GetAllWorkShiftsQuery received");

        List<WorkShift> workShifts = workShiftRepository.findAll();
        return workShifts.stream()
                .map(this::mapToResponseModel)
                .collect(Collectors.toList());
    }

    @QueryHandler
    public WorkShiftResponseModel handle(GetWorkShiftByIdQuery query) throws Exception {
        log.info("GetWorkShiftByIdQuery received - ID: {}", query.getId());

        WorkShift workShift = workShiftRepository.findById(query.getId())
                .orElseThrow(() -> new Exception("WorkShift not found: " + query.getId()));

        return mapToResponseModel(workShift);
    }

    @QueryHandler
    public List<WorkShiftResponseModel> handle(GetWorkShiftsByEmployeeQuery query) {
        log.info("GetWorkShiftsByEmployeeQuery received - EmployeeID: {}", query.getEmployeeId());

        List<WorkShift> workShifts = workShiftRepository.findByEmployeeId(query.getEmployeeId());
        return workShifts.stream()
                .map(this::mapToResponseModel)
                .collect(Collectors.toList());
    }

    private WorkShiftResponseModel mapToResponseModel(WorkShift workShift) {
        WorkShiftResponseModel model = new WorkShiftResponseModel();
        model.setId(workShift.getId());
        model.setEmployeeId(workShift.getEmployee() != null ? workShift.getEmployee().getId() : null);
        model.setShiftName(workShift.getShiftName());
        model.setStartTime(workShift.getStartTime());
        model.setEndTime(workShift.getEndTime());
        model.setIsAttended(workShift.getIsAttended());
        return model;
    }
}
