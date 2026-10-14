package com.foogaro.hotel.common.config;

import com.foogaro.hotel.common.session.SessionConstants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(
            @Value("${spring.application.name:service}") String applicationName,
            @Value("${springdoc.info.description:Hotel booking microservice API}") String description) {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName)
                        .version("1.0.0")
                        .description(description));
    }

    @Bean
    public OperationCustomizer sessionHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            boolean alreadyPresent = operation.getParameters() != null
                    && operation.getParameters().stream()
                    .anyMatch(parameter -> SessionConstants.SESSION_HEADER.equalsIgnoreCase(parameter.getName()));
            if (!alreadyPresent) {
                operation.addParametersItem(new Parameter()
                        .in("header")
                        .name(SessionConstants.SESSION_HEADER)
                        .required(false)
                        .description("Anonymous session id. Auto-generated when omitted.")
                        .schema(new StringSchema()));
            }
            return operation;
        };
    }
}
