package com.main_project.jobflow.worker;

import com.main_project.jobflow.events.JobCreatedEvent;
import com.main_project.jobflow.services.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class JobWorker {
    private final JobService jobService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleJobCreated(JobCreatedEvent jobCreatedEvent) {
        jobService.markAsProcessing(jobCreatedEvent.jobId());
        System.out.println("ricevuto job "+jobCreatedEvent.jobId() +" su " + Thread.currentThread().getName());
    }




}
