package com.main_project.jobflow.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record APIError(
        Error errorName,
        String path,
        String msg,
        HttpStatus status,
        Instant timestamp
) {
}
