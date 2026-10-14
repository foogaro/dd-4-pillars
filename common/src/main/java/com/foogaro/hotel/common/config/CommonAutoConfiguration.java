package com.foogaro.hotel.common.config;

import com.foogaro.hotel.common.exception.GlobalExceptionHandler;
import com.foogaro.hotel.common.session.SessionFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        JacksonConfig.class,
        ObservabilityConfig.class,
        OpenApiConfig.class,
        RestClientConfig.class,
        SessionFilter.class,
        GlobalExceptionHandler.class
})
public class CommonAutoConfiguration {
}
