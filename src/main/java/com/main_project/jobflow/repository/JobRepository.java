package com.main_project.jobflow.repository;

import com.main_project.jobflow.models.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> getById(long id);
}
