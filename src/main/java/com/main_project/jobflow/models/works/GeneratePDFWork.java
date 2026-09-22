package com.main_project.jobflow.models.works;

import com.main_project.jobflow.models.Work;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "generate_pdf_works")

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class GeneratePDFWork extends Work {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String textBody;



}
