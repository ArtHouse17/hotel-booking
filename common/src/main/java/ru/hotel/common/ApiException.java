package ru.hotel.common;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final UUID bookingId;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public ApiException(HttpStatus status, String message, UUID bookingId) {
        super(message);
        this.status = status;
        this.bookingId = bookingId;
    }

    public HttpStatus status() {
        return status;
    }

    public UUID bookingId() {
        return bookingId;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
