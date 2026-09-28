package com.main_project.jobflow.models.results;

import com.main_project.jobflow.models.Result;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "generated_pdf_results")

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class GeneratedPDFResult extends Result {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String textBody;
}
