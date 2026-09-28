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

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "work_id")
    private Work work;

    @OneToOne
    @JoinColumn(name = "result_id")
    private Result result;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusJob status;
}
