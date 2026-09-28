package com.main_project.jobflow.services;

import com.main_project.jobflow.dto.CreatePdfJobRequest;
import com.main_project.jobflow.models.Job;
import com.main_project.jobflow.models.StatusJob;

import com.main_project.jobflow.models.works.GeneratePdfWork;
import com.main_project.jobflow.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;

    public Job createPdfJob(CreatePdfJobRequest request) {
        GeneratePdfWork work = GeneratePdfWork.builder()
                .title(request.title())
                .textBody(request.textBody())
                .build();

        Job job = Job.builder()
                .name(request.name())
                .status(StatusJob.CREATED)
                .work(work)
                .build();
        return jobRepository.save(job);

    }
}
