package ru.hotel.hotel.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
import lombok.*;

/** Local projection of booking dates; released rows are idempotency tombstones. */
@Entity
@Table(
        name = "room_reservations",
        indexes =
                @Index(
                        name = "idx_slot_overlap",
                        columnList = "room_id,active,start_date,end_date"))
@Getter
@Setter
@NoArgsConstructor
public class RoomReservation {
    @Id private UUID bookingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public RoomReservation(UUID id, Room room, LocalDate start, LocalDate end, boolean active) {
        this.bookingId = id;
        this.room = room;
        this.startDate = start;
        this.endDate = end;
        this.active = active;
    }
}
