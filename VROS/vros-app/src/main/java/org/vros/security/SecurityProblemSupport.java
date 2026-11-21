package org.vros.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Emits RFC 7807 {@code application/problem+json} responses for security failures,
 * keeping 401/403 payloads consistent with {@code GlobalExceptionHandler}.
 */
@Component
public class SecurityProblemSupport implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Prefers the application-configured Jackson 2 mapper when present, and otherwise
     * falls back to a plain mapper (Spring Boot 4 auto-configures Jackson 3 by default).
     */
    public SecurityProblemSupport(ObjectProvider<ObjectMapper> objectMapperProvider) {
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                authException.getMessage() != null
                        ? authException.getMessage()
                        : "Full authentication is required to access this resource");
        pd.setTitle("Unauthorized");
        pd.setType(URI.create("https://vros.example/errors/unauthorized"));
        pd.setInstance(URI.create(request.getRequestURI()));

        writeResponse(response, HttpStatus.UNAUTHORIZED, pd);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Access is denied: insufficient role privileges or ownership mismatch");
        pd.setTitle("Forbidden");
        pd.setType(URI.create("https://vros.example/errors/forbidden"));
        pd.setInstance(URI.create(request.getRequestURI()));

        writeResponse(response, HttpStatus.FORBIDDEN, pd);
    }

    private void writeResponse(HttpServletResponse response, HttpStatus status, ProblemDetail pd)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), pd);
    }
}
