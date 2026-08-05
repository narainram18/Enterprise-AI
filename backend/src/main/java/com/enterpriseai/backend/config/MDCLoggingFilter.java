package com.enterpriseai.backend.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Just after Security filter
public class MDCLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String USER_EMAIL = "userEmail";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString();
            }
            MDC.put(TRACE_ID, traceId);
            response.setHeader("X-Trace-Id", traceId);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getName() != null) {
                MDC.put(USER_EMAIL, authentication.getName());
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
