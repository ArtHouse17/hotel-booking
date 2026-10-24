package ru.hotel.common;

import java.time.Instant;
import java.util.Map;

public record ErrorDto(
        Instant timestamp,
        int status,
        String message,
        String path,
        String traceId,
        Map<String, String> errors) {}
