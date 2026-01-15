package com.cinema.movies.query.projection;

import com.cinema.movies.command.data.Cinema;
import com.cinema.movies.command.data.Employee;
import com.cinema.movies.command.data.Reponsitory.CinemaRepository;
import com.cinema.movies.command.data.Reponsitory.EmployeeRepository;
import com.cinema.movies.command.event.EmployeeCreatedEvent;
import com.cinema.movies.command.event.EmployeeDeletedEvent;
import com.cinema.movies.command.event.EmployeeUpdatedEvent;
import com.cinema.movies.query.model.EmployeeResponseModel;
import com.cinema.movies.query.queries.GetAllEmployeesQuery;
import com.cinema.movies.query.queries.GetEmployeeByIdQuery;
import com.cinema.movies.query.queries.GetEmployeesByCinemaQuery;
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
public class EmployeeProjection {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    /*
     * =======================
     * EVENT HANDLERS
     * =======================
     */

    @EventHandler
    public void on(EmployeeCreatedEvent event) {
        log.info("EmployeeCreatedEvent received - ID: {}", event.getId());

        // Idempotent - check exists
        if (employeeRepository.existsById(event.getId())) {
            log.warn("Employee already exists: {}", event.getId());
            return;
        }

        Cinema cinema = cinemaRepository.findById(event.getCinemaId())
                .orElseThrow(() -> new RuntimeException("Cinema not found: " + event.getCinemaId()));

        Employee employee = Employee.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .cinema(cinema)
                .position(event.getPosition())
                .status(event.getStatus() != null ? event.getStatus() : "ACTIVE")
                .build();

        employeeRepository.save(employee);
    }

    @EventHandler
    public void on(EmployeeUpdatedEvent event) {
        log.info("EmployeeUpdatedEvent received - ID: {}", event.getId());

        Employee employee = employeeRepository.findById(event.getId())
                .orElseThrow(() -> new RuntimeException("Employee not found: " + event.getId()));

        Cinema cinema = cinemaRepository.findById(event.getCinemaId())
                .orElseThrow(() -> new RuntimeException("Cinema not found: " + event.getCinemaId()));

        employee.setUserId(event.getUserId());
        employee.setCinema(cinema);
        employee.setPosition(event.getPosition());
        employee.setStatus(event.getStatus());

        employeeRepository.save(employee);
    }

    @EventHandler
    public void on(EmployeeDeletedEvent event) {
        log.info("EmployeeDeletedEvent received - ID: {}", event.getId());

        if (employeeRepository.existsById(event.getId())) {
            employeeRepository.deleteById(event.getId());
        } else {
            log.warn("Employee already deleted: {}", event.getId());
        }
    }

    /*
     * =======================
     * QUERY HANDLERS
     * =======================
     */

    @QueryHandler
    public List<EmployeeResponseModel> handle(GetAllEmployeesQuery query) {
        log.info("GetAllEmployeesQuery received");

        List<Employee> employees = employeeRepository.findAll();
        return employees.stream()
                .map(this::mapToResponseModel)
                .collect(Collectors.toList());
    }

    @QueryHandler
    public EmployeeResponseModel handle(GetEmployeeByIdQuery query) throws Exception {
        log.info("GetEmployeeByIdQuery received - ID: {}", query.getId());

        Employee employee = employeeRepository.findById(query.getId())
                .orElseThrow(() -> new Exception("Employee not found: " + query.getId()));

        return mapToResponseModel(employee);
    }

    @QueryHandler
    public List<EmployeeResponseModel> handle(GetEmployeesByCinemaQuery query) {
        log.info("GetEmployeesByCinemaQuery received - CinemaID: {}", query.getCinemaId());

        List<Employee> employees = employeeRepository.findByCinemaId(query.getCinemaId());
        return employees.stream()
                .map(this::mapToResponseModel)
                .collect(Collectors.toList());
    }

    private EmployeeResponseModel mapToResponseModel(Employee employee) {
        EmployeeResponseModel model = new EmployeeResponseModel();
        model.setId(employee.getId());
        model.setUserId(employee.getUserId());
        model.setCinemaId(employee.getCinema() != null ? employee.getCinema().getId() : null);
        model.setPosition(employee.getPosition());
        model.setStatus(employee.getStatus());
        model.setJoinedAt(employee.getJoinedAt());
        return model;
    }
}
