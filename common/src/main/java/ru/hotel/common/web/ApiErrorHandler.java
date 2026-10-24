package ru.hotel.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.hotel.common.*;

@RestControllerAdvice
public class ApiErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorDto> business(ApiException e, HttpServletRequest request) {
        return error(e.status(), e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DomainConflictException.class)
    ResponseEntity<ErrorDto> domainConflict(DomainConflictException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorDto> validation(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult()
                .getFieldErrors()
                .forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Validation failed", request, fields);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class
    })
    ResponseEntity<ErrorDto> malformed(Exception e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request or parameter", request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorDto> conflict(Exception e, HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "Data conflict: resource already exists or is referenced",
                request,
                Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorDto> forbidden(Exception e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Access denied", request, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorDto> unauthorized(Exception e, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Authentication failed", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorDto> unexpected(Exception e, HttpServletRequest request) {
        if (e instanceof org.springframework.web.ErrorResponse webError) {
            HttpStatus status = HttpStatus.resolve(webError.getStatusCode().value());
            if (status != null) return error(status, status.getReasonPhrase(), request, Map.of());
        }
        log.error("event=unexpected_error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request, Map.of());
    }

    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    ResponseEntity<ErrorDto> concurrent(Exception e, HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT, "Concurrent update; retry the request", request, Map.of());
    }

    private ResponseEntity<ErrorDto> error(
            HttpStatus status, String message, HttpServletRequest req, Map<String, String> fields) {
        return ResponseEntity.status(status)
                .body(
                        new ErrorDto(
                                Instant.now(),
                                status.value(),
                                message,
                                req.getRequestURI(),
                                MDC.get("traceId"),
                                fields));
    }
}
