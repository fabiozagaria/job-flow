package com.main_project.jobflow;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("JobFlow API")
                        .version("0.1.0")
                        .description("""
                                Crea un job con POST /jobs e seguine lo stato con GET /jobs/{id}.
                                Il job viene elaborato in modo asincrono: CREATED → PROCESSING → COMPLETED / FAILED.""")
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
