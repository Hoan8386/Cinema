package com.cinema.movies.command.controller;

import java.util.UUID;

import com.cinema.movies.command.data.Movie;
import com.cinema.movies.command.data.Reponsitory.MovieRepository;
import com.cinema.movies.service.MinioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.cinema.commonservice.annotation.ApiMessage;
import com.cinema.movies.command.command.CreateMovieCommand;
import com.cinema.movies.command.command.UpdateMovieCommand;
import com.cinema.movies.command.command.DeleteMovieCommand;
import com.cinema.movies.command.model.CommandResponse;
import com.cinema.movies.command.model.MovieRequestModel;

@RestController
@RequestMapping("/api/v1/movies")
@Slf4j
public class MovieCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private MinioService minioService;

    @Autowired
    private MovieRepository movieRepository;

    @PostMapping
    @ApiMessage("Tạo phim thành công")
    public CommandResponse createMovie(@Valid @RequestBody MovieRequestModel model) {

        String id = UUID.randomUUID().toString();

        log.info("Received request - Title: {}, Description: {}, Duration: {}",
                model.getTitle(), model.getDescription(), model.getDuration());

        CreateMovieCommand command = new CreateMovieCommand(
                id,
                model.getTitle(),
                model.getDescription(),
                model.getDuration(),
                model.getPosterUrl());

        try {
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to create movie: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo phim: " + e.getMessage());
        }

        return new CommandResponse(id);
    }

    @PostMapping(value = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiMessage("Tạo phim với poster thành công")
    public CommandResponse createMovieWithFile(
            @RequestParam("title") @NotBlank String title,
            @RequestParam("description") String description,
            @RequestParam("duration") @Min(1) Integer duration,
            @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {

        String id = UUID.randomUUID().toString();
        String posterUrl = null;

        // Upload poster nếu có file
        if (file != null && !file.isEmpty()) {
            log.info("Uploading poster for new movie: {}", title);
            posterUrl = minioService.uploadFile(file);
        }

        log.info("Creating movie - Title: {}, Duration: {}, PosterUrl: {}",
                title, duration, posterUrl);

        CreateMovieCommand command = new CreateMovieCommand(
                id,
                title,
                description,
                duration,
                posterUrl);

        try {
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to create movie with file: {}", e.getMessage(), e);
            // Xóa file đã upload nếu command fail
            if (posterUrl != null) {
                try {
                    minioService.deleteFileByUrl(posterUrl);
                } catch (Exception ex) {
                    log.warn("Failed to cleanup uploaded file: {}", ex.getMessage());
                }
            }
            throw new RuntimeException("Không thể tạo phim: " + e.getMessage());
        }

        return new CommandResponse(id);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật phim thành công")
    public CommandResponse updateMovie(
            @PathVariable String id,
            @Valid @RequestBody MovieRequestModel model) {

        // Xóa poster cũ nếu có thay đổi posterUrl
        try {
            Movie existingMovie = movieRepository.findById(id).orElse(null);
            if (existingMovie != null && existingMovie.getPosterUrl() != null
                    && !existingMovie.getPosterUrl().equals(model.getPosterUrl())) {
                minioService.deleteFileByUrl(existingMovie.getPosterUrl());
                log.info("Deleted old poster for movie: {}", id);
            }
        } catch (Exception e) {
            log.warn("Failed to delete old poster: {}", e.getMessage());
        }

        UpdateMovieCommand command = new UpdateMovieCommand(
                id,
                model.getTitle(),
                model.getDescription(),
                model.getDuration(),
                model.getPosterUrl());

        try {
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to update movie: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể cập nhật phim: " + e.getMessage());
        }

        return new CommandResponse(id);
    }

    @PutMapping(value = "/{id}/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiMessage("Cập nhật phim với poster thành công")
    public CommandResponse updateMovieWithFile(
            @PathVariable String id,
            @RequestParam("title") @NotBlank String title,
            @RequestParam("description") String description,
            @RequestParam("duration") @Min(1) Integer duration,
            @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {

        String posterUrl = null;

        // Lấy movie hiện tại
        Movie existingMovie = movieRepository.findById(id).orElse(null);

        // Upload poster mới nếu có file
        if (file != null && !file.isEmpty()) {
            log.info("Uploading new poster for movie: {}", id);
            posterUrl = minioService.uploadFile(file);

            // Xóa poster cũ
            if (existingMovie != null && existingMovie.getPosterUrl() != null) {
                try {
                    minioService.deleteFileByUrl(existingMovie.getPosterUrl());
                    log.info("Deleted old poster for movie: {}", id);
                } catch (Exception e) {
                    log.warn("Failed to delete old poster: {}", e.getMessage());
                }
            }
        } else if (existingMovie != null) {
            // Giữ nguyên poster cũ nếu không upload file mới
            posterUrl = existingMovie.getPosterUrl();
        }

        log.info("Updating movie - ID: {}, Title: {}, PosterUrl: {}",
                id, title, posterUrl);

        UpdateMovieCommand command = new UpdateMovieCommand(
                id,
                title,
                description,
                duration,
                posterUrl);

        try {
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to update movie with file: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể cập nhật phim: " + e.getMessage());
        }

        return new CommandResponse(id);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa phim thành công")
    public CommandResponse deleteMovie(@PathVariable String id) {

        // Xóa poster trước khi xóa movie
        try {
            Movie movie = movieRepository.findById(id).orElse(null);
            if (movie != null && movie.getPosterUrl() != null) {
                minioService.deleteFileByUrl(movie.getPosterUrl());
                log.info("Deleted poster for movie: {}", id);
            }
        } catch (Exception e) {
            log.warn("Failed to delete poster: {}", e.getMessage());
        }

        DeleteMovieCommand command = new DeleteMovieCommand(id);

        try {
            commandGateway.sendAndWait(command);
        } catch (Exception e) {
            log.error("Failed to delete movie: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể xóa phim: " + e.getMessage());
        }

        return new CommandResponse(id);
    }
}
