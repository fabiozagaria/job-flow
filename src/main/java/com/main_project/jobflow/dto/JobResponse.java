package com.main_project.jobflow.dto;

import com.main_project.jobflow.models.StatusJob;

public record JobResponse(
        Long id,
        String name,
        StatusJob status
) {
}
