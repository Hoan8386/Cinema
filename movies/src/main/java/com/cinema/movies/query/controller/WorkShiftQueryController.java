package com.cinema.movies.query.controller;

import com.cinema.movies.query.model.WorkShiftResponseModel;
import com.cinema.movies.query.queries.GetAllWorkShiftsQuery;
import com.cinema.movies.query.queries.GetWorkShiftByIdQuery;
import com.cinema.movies.query.queries.GetWorkShiftsByEmployeeQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workshifts")
@Slf4j
public class WorkShiftQueryController {

    @Autowired
    private QueryGateway queryGateway;

    @GetMapping
    public List<WorkShiftResponseModel> getAllWorkShifts() {
        log.info("GET /api/v1/workshifts - Get all work shifts");

        GetAllWorkShiftsQuery query = new GetAllWorkShiftsQuery();
        return queryGateway.query(
                query,
                ResponseTypes.multipleInstancesOf(WorkShiftResponseModel.class)).join();
    }

    @GetMapping("/{id}")
    public WorkShiftResponseModel getWorkShiftById(@PathVariable String id) {
        log.info("GET /api/v1/workshifts/{} - Get work shift by id", id);

        GetWorkShiftByIdQuery query = new GetWorkShiftByIdQuery(id);
        return queryGateway.query(
                query,
                ResponseTypes.instanceOf(WorkShiftResponseModel.class)).join();
    }

    @GetMapping("/employee/{employeeId}")
    public List<WorkShiftResponseModel> getWorkShiftsByEmployee(@PathVariable String employeeId) {
        log.info("GET /api/v1/workshifts/employee/{} - Get work shifts by employee", employeeId);

        GetWorkShiftsByEmployeeQuery query = new GetWorkShiftsByEmployeeQuery(employeeId);
        return queryGateway.query(
                query,
                ResponseTypes.multipleInstancesOf(WorkShiftResponseModel.class)).join();
    }
}
