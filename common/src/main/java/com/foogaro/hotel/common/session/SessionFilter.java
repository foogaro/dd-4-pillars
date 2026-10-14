package com.foogaro.hotel.common.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SessionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String sessionId = request.getHeader(SessionConstants.SESSION_HEADER);
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        SessionContext.set(sessionId);
        MDC.put(SessionConstants.SESSION_MDC_KEY, sessionId);
        response.setHeader(SessionConstants.SESSION_HEADER, sessionId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SessionContext.clear();
            MDC.remove(SessionConstants.SESSION_MDC_KEY);
        }
    }
}
