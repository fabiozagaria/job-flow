package com.main_project.jobflow.controllers;

import com.main_project.jobflow.dto.CreatePdfJobRequest;
import com.main_project.jobflow.dto.JobResponse;
import com.main_project.jobflow.models.Job;
import com.main_project.jobflow.services.JobService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    @GetMapping("/{id}")
    public JobResponse getById(
            @PathVariable long id
    ) {
        Job job = jobService.findById(id);
        return new JobResponse(
                job.getId(),
                job.getName(),
                job.getStatus()
        );

    }

    @PostMapping
    public ResponseEntity<JobResponse> post(
            @Valid @RequestBody CreatePdfJobRequest createPdfJobRequest,
            HttpServletRequest request
            )
    {
        Job job = jobService.createPdfJob(createPdfJobRequest);

        return ResponseEntity
                .created(ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(job.getId())
                        .toUri())
                .body(new JobResponse(job.getId(), job.getName(), job.getStatus()));
    }

}
