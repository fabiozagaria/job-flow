package com.main_project.jobflow.repository;

import com.main_project.jobflow.models.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
}
