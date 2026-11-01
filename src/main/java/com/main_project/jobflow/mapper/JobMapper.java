package com.main_project.jobflow.mapper;

import com.main_project.jobflow.dto.JobResponse;
import com.main_project.jobflow.models.Job;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface JobMapper {
    JobResponse toResponse(Job job);
}
