package com.cinema.movies.command.data;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cinemas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cinema {

    @Id
    private String id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;
}