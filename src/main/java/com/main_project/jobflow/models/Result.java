package com.main_project.jobflow.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "results")
@Inheritance(strategy = InheritanceType.JOINED)

@NoArgsConstructor
public abstract class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

}
