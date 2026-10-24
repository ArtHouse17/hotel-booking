package ru.hotel.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ErrorDto(
        Instant timestamp,
        int status,
        String message,
        String path,
        String traceId,
        Map<String, String> errors,
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID bookingId) {
    public ErrorDto(
            Instant timestamp,
            int status,
            String message,
            String path,
            String traceId,
            Map<String, String> errors) {
        this(timestamp, status, message, path, traceId, errors, null);
    }
}
