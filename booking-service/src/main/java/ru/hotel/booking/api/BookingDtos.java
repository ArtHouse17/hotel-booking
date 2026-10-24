package ru.hotel.booking.api;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import ru.hotel.booking.domain.BookingStatus;

public final class BookingDtos {
    private BookingDtos() {}

    public record CreateBooking(
            @NotNull UUID requestId,
            @NotNull Boolean autoSelect,
            @Positive Long roomId,
            @Positive Long hotelId,
            @NotNull @FutureOrPresent LocalDate startDate,
            @NotNull @Future LocalDate endDate) {
        public String fingerprint() {
            return autoSelect
                    + "|"
                    + (Boolean.TRUE.equals(autoSelect) ? null : roomId)
                    + "|"
                    + hotelId
                    + "|"
                    + startDate
                    + "|"
                    + endDate;
        }
    }

    public record BookingView(
            UUID id,
            UUID requestId,
            Long userId,
            Long roomId,
            Long hotelId,
            boolean autoSelect,
            LocalDate startDate,
            LocalDate endDate,
            BookingStatus status,
            Instant createdAt,
            boolean compensationRequired,
            String failureReason,
            Integer failureStatus) {}

    public record BookingOutcome(BookingView booking, boolean created) {}
}
