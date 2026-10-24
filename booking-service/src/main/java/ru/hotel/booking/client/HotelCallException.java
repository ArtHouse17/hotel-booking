package ru.hotel.booking.client;

public class HotelCallException extends RuntimeException {
    private final int status;

    public HotelCallException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
