package com.cinema.movies.query.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetWorkShiftsByEmployeeQuery {
    private String employeeId;
}
