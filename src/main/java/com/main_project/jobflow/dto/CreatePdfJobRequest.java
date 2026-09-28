package com.main_project.jobflow.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePdfJobRequest(
        @NotBlank
        @Size(max = 25)
        String name,

        @NotBlank
        @Size(max = 25)
        String title,

        @NotBlank
        @Size(max = 500)
        String textBody
) {

}
