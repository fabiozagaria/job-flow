package com.main_project.jobflow.models;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "works")
@Inheritance(strategy = InheritanceType.JOINED)

@NoArgsConstructor
public abstract class Work {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
