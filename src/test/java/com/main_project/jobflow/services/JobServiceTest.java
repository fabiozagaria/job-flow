package com.main_project.jobflow.services;

import com.main_project.jobflow.dto.CreatePdfJobRequest;
import com.main_project.jobflow.events.JobCreatedEvent;
import com.main_project.jobflow.exception.JobNotFoundException;
import com.main_project.jobflow.models.Job;
import com.main_project.jobflow.models.StatusJob;
import com.main_project.jobflow.models.works.GeneratePdfWork;
import com.main_project.jobflow.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private JobService jobService;

    @Test
    void createPdfJob_savesJobInCreatedStatusWithPdfWork() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job result = jobService.createPdfJob(new CreatePdfJobRequest("job1", "titolo", "corpo"));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("job1");
        assertThat(saved.getStatus()).isEqualTo(StatusJob.CREATED);
        assertThat(saved.getWork()).isInstanceOf(GeneratePdfWork.class);
        GeneratePdfWork work = (GeneratePdfWork) saved.getWork();
        assertThat(work.getTitle()).isEqualTo("titolo");
        assertThat(work.getTextBody()).isEqualTo("corpo");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void createPdfJob_publishesJobCreatedEventWithSavedId() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            j.setId(7L);
            return j;
        });

        jobService.createPdfJob(new CreatePdfJobRequest("job1", "titolo", "corpo"));

        verify(eventPublisher).publishEvent(new JobCreatedEvent(7L));
    }

    @Test
    void findById_returnsJobWhenPresent() {
        Job job = Job.builder().id(1L).name("job1").status(StatusJob.CREATED).build();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThat(jobService.findById(1L)).isSameAs(job);
    }

    @Test
    void findById_throwsWhenMissing() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.findById(99L))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void markAsProcessing_movesCreatedJobToProcessing() {
        Job job = Job.builder().id(1L).name("job1").status(StatusJob.CREATED).build();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        jobService.markAsProcessing(job.getId());
        assertThat(job.getStatus()).isEqualTo(StatusJob.PROCESSING);

    }
}
