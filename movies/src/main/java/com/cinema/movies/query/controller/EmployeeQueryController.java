package com.cinema.movies.query.controller;

import com.cinema.movies.query.model.EmployeeResponseModel;
import com.cinema.movies.query.queries.GetAllEmployeesQuery;
import com.cinema.movies.query.queries.GetEmployeeByIdQuery;
import com.cinema.movies.query.queries.GetEmployeesByCinemaQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@Slf4j
public class EmployeeQueryController {

    @Autowired
    private QueryGateway queryGateway;

    @GetMapping
    public List<EmployeeResponseModel> getAllEmployees() {
        log.info("GET /api/v1/employees - Get all employees");

        GetAllEmployeesQuery query = new GetAllEmployeesQuery();
        return queryGateway.query(
                query,
                ResponseTypes.multipleInstancesOf(EmployeeResponseModel.class)).join();
    }

    @GetMapping("/{id}")
    public EmployeeResponseModel getEmployeeById(@PathVariable String id) {
        log.info("GET /api/v1/employees/{} - Get employee by id", id);

        GetEmployeeByIdQuery query = new GetEmployeeByIdQuery(id);
        return queryGateway.query(
                query,
                ResponseTypes.instanceOf(EmployeeResponseModel.class)).join();
    }

    @GetMapping("/cinema/{cinemaId}")
    public List<EmployeeResponseModel> getEmployeesByCinema(@PathVariable String cinemaId) {
        log.info("GET /api/v1/employees/cinema/{} - Get employees by cinema", cinemaId);

        GetEmployeesByCinemaQuery query = new GetEmployeesByCinemaQuery(cinemaId);
        return queryGateway.query(
                query,
                ResponseTypes.multipleInstancesOf(EmployeeResponseModel.class)).join();
    }
}
