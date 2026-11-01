package com.main_project.jobflow.services;

import com.main_project.jobflow.dto.CreatePdfJobRequest;
import com.main_project.jobflow.events.JobCreatedEvent;
import com.main_project.jobflow.exception.JobNotFoundException;
import com.main_project.jobflow.models.Job;
import com.main_project.jobflow.models.StatusJob;

import com.main_project.jobflow.models.works.GeneratePdfWork;
import com.main_project.jobflow.repository.JobRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Job createPdfJob(CreatePdfJobRequest request) {
        GeneratePdfWork work = GeneratePdfWork.builder()
                .title(request.title())
                .textBody(request.textBody())
                .build();

        Job newJob = Job.builder()
                .name(request.name())
                .status(StatusJob.CREATED)
                .work(work)
                .build();
        var job = jobRepository.save(newJob);
        applicationEventPublisher.publishEvent(
                new JobCreatedEvent(job.getId())
        );
        return job;

    }

    @Transactional(readOnly = true)
    public Job findById(long id) {
        return jobRepository.findById(id)
                .orElseThrow( ()-> new JobNotFoundException("Id non trovato!"));
    }
}
