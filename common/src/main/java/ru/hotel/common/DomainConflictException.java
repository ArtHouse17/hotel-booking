package ru.hotel.common;

public class DomainConflictException extends RuntimeException {
    public DomainConflictException(String message) { super(message); }
}
