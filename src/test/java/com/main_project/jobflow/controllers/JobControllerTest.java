package com.main_project.jobflow.controllers;

import com.main_project.jobflow.exception.GlobalExceptionHandler;
import com.main_project.jobflow.exception.JobNotFoundException;
import com.main_project.jobflow.mapper.JobMapperImpl;
import com.main_project.jobflow.models.Job;
import com.main_project.jobflow.models.StatusJob;
import com.main_project.jobflow.dto.CreatePdfJobRequest;
import com.main_project.jobflow.services.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import({GlobalExceptionHandler.class, JobMapperImpl.class})
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @Test
    void getById_returnsJob() throws Exception {
        when(jobService.findById(1L)).thenReturn(
                Job.builder().id(1L).name("job1").status(StatusJob.CREATED).build());

        mockMvc.perform(get("/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("job1"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        when(jobService.findById(99L)).thenThrow(new JobNotFoundException("Id non trovato!"));

        mockMvc.perform(get("/jobs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorName").value("JOB_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/jobs/99"))
                .andExpect(jsonPath("$.msg").value("Id non trovato!"));
    }

    @Test
    void post_createsJobAndReturnsLocation() throws Exception {
        when(jobService.createPdfJob(any(CreatePdfJobRequest.class))).thenReturn(
                Job.builder().id(5L).name("job1").status(StatusJob.CREATED).build());

        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"job1","title":"titolo","textBody":"corpo"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/jobs/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void post_returns400OnBlankName() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","title":"titolo","textBody":"corpo"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_returns400OnTooLongTitle() throws Exception {
        String longTitle = "x".repeat(26);
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"job1","title":"%s","textBody":"corpo"}
                                """.formatted(longTitle)))
                .andExpect(status().isBadRequest());
    }
}
