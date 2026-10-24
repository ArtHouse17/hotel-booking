package ru.hotel.booking.domain;

import java.time.*;
import java.util.UUID;
import lombok.*;
import ru.hotel.common.DomainConflictException;

@Getter @Setter @NoArgsConstructor
public class Booking {
    private UUID id = UUID.randomUUID();
    private User user;
    private UUID requestId;
    private String fingerprint;
    private Long roomId;
    private Long hotelId;
    private boolean autoSelect;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status = BookingStatus.PENDING;
    private Instant createdAt = Instant.now();
    private boolean compensationRequired;
    private String failureReason;
    private Integer failureStatus;

    public void selectRoom(Long roomId) {
        requirePending();
        this.roomId = roomId;
    }

    public void confirm() {
        requirePending();
        status = BookingStatus.CONFIRMED;
    }

    public void cancel(String reason, int errorStatus) {
        if (status == BookingStatus.CANCELLED) return;
        status = BookingStatus.CANCELLED;
        failureReason = reason;
        failureStatus = errorStatus;
        compensationRequired = roomId != null;
    }

    public void compensated() {
        if (status == BookingStatus.CANCELLED) compensationRequired = false;
    }

    private void requirePending() {
        if (status != BookingStatus.PENDING) throw new DomainConflictException("Booking is no longer pending");
    }
}
