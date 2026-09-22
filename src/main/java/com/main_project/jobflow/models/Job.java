package com.main_project.jobflow.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "jobs")

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToOne
    @JoinColumn(name = "work_id")
    private Work work;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusJob status;
}
