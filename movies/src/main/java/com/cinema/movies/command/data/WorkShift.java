package com.cinema.movies.command.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_shifts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkShift {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "shift_name", length = 50)
    private String shiftName; // Sáng, Chiều, Tối

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_attended")
    private Boolean isAttended; // Điểm danh: true/false
}
