package com.cinema.movies.command.data;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "show_times")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowTime {

    @Id
    private String id;

    // Mapping quan hệ n-1 với Movie
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    // Mapping quan hệ n-1 với Cinema
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;
}