package org.vros.api.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.vros.domain.exception.ConflictException;
import org.vros.domain.exception.ResourceNotFoundException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Global REST exception handler that leverages Resource Bundles (MessageSource)
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, Locale locale) {
        String detail = (ex.getMessageKey() != null)
                ? messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), ex.getMessage(), locale)
                : ex.getMessage();
        String title = messageSource.getMessage("error.not_found.title", null, "Resource Not Found", locale);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail);
        pd.setType(URI.create("https://vros.example/errors/not-found"));
        pd.setTitle(title);
        return pd;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, Locale locale) {
        String detail = (ex.getMessageKey() != null)
                ? messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), ex.getMessage(), locale)
                : ex.getMessage();
        String title = messageSource.getMessage("error.conflict.title", null, "Conflict", locale);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        pd.setType(URI.create("https://vros.example/errors/conflict"));
        pd.setTitle(title);
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, Locale locale) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String title = messageSource.getMessage("error.validation.title", null, "Validation Failed", locale);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, details);
        pd.setType(URI.create("https://vros.example/errors/validation"));
        pd.setTitle(title);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, Locale locale) {
        String title = messageSource.getMessage("error.constraint.title", null, "Constraint Violation", locale);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("https://vros.example/errors/validation"));
        pd.setTitle(title);
        return pd;
    }
}
