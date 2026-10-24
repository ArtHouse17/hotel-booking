package ru.hotel.hotel.domain;

import lombok.*;
import ru.hotel.common.DomainConflictException;

@Getter @Setter @NoArgsConstructor
public class Room {
    private Long id;
    private Hotel hotel;
    private String number;
    private boolean available = true;
    private long timesBooked;

    public Room(Hotel hotel, String number, boolean available) {
        this.hotel = hotel;
        this.number = number;
        this.available = available;
    }

    public void book() {
        if (!available) throw new DomainConflictException("Room is out of service");
        timesBooked++;
    }

    public void release() { timesBooked = Math.max(0, timesBooked - 1); }
}
