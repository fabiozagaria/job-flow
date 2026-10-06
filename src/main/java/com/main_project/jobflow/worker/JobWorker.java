package com.main_project.jobflow.worker;

import com.main_project.jobflow.services.JobService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JobWorker {

    private final JobService jobService;


}
