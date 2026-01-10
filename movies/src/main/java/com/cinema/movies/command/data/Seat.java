package com.cinema.movies.command.data;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    private String id;

    // Mapping quan hệ n-1 với Cinema
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Column(name = "seat_row", length = 5)
    private String seatRow; // Ví dụ: A, B, C

    @Column(name = "seat_number")
    private Integer seatNumber; // Ví dụ: 1, 2, 3
}