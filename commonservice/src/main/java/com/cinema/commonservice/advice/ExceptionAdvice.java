package com.cinema.commonservice.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.cinema.commonservice.model.ErrorResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

@ControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ErrorResponse> handleCompletionException(CompletionException ex) {

        Throwable cause = ex.getCause();
        String message = cause != null ? cause.getMessage() : ex.getMessage();

        ErrorResponse error = new ErrorResponse(
                "MOVIE_NOT_FOUND", // business code
                message,
                HttpStatus.NOT_FOUND.value(),
                null // không có detail
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });

        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Invalid request data",
                HttpStatus.BAD_REQUEST.value(),
                fieldErrors);

        return ResponseEntity
                .badRequest()
                .body(error);
    }

}
