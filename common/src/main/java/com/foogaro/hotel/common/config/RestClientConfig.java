package com.foogaro.hotel.common.config;

import com.foogaro.hotel.common.session.SessionConstants;
import com.foogaro.hotel.common.session.SessionContext;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestClientConfig {

    @Bean
    RestClientCustomizer sessionHeaderRestClientCustomizer() {
        return builder -> builder.requestInterceptor((request, body, execution) -> {
            String sessionId = SessionContext.get();
            if (sessionId != null && !sessionId.isBlank()) {
                request.getHeaders().set(SessionConstants.SESSION_HEADER, sessionId);
            }
            return execution.execute(request, body);
        });
    }
}
