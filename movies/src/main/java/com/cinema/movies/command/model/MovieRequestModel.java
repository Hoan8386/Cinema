package com.cinema.movies.command.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MovieRequestModel {

    private Long id;

    private String title;

    private String description;

    private Integer duration; // Thời lượng tính bằng phút

    private String posterUrl;

}
