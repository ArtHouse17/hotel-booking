package ru.hotel.hotel.domain;

import java.time.*;
import java.util.UUID;
import lombok.*;
import ru.hotel.common.DomainConflictException;

@Getter @Setter @NoArgsConstructor
public class RoomReservation {
    private UUID bookingId;
    private Room room;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private Instant createdAt = Instant.now();

    public RoomReservation(UUID id, Room room, LocalDate start, LocalDate end, boolean active) {
        this.bookingId = id;
        this.room = room;
        this.startDate = start;
        this.endDate = end;
        this.active = active;
    }

    public void verifyData(Long roomId, LocalDate start, LocalDate end) {
        if (!room.getId().equals(roomId) || !startDate.equals(start) || !endDate.equals(end)) {
            throw new DomainConflictException("bookingId was already used with different reservation data");
        }
    }

    public void requireActive() {
        if (!active) throw new DomainConflictException("Reservation has already been released");
    }

    public boolean release() {
        if (!active) return false;
        active = false;
        return true;
    }
}
