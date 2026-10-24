package ru.hotel.booking.api;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import ru.hotel.booking.api.BookingDtos.*;
import ru.hotel.booking.service.*;
import ru.hotel.common.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class BookingController {
    private final BookingService service;
    private final BookingTransactions transactions;

    public BookingController(BookingService service, BookingTransactions transactions) {
        this.service = service;
        this.transactions = transactions;
    }

    @PostMapping({"/api/bookings", "/booking"})
    public ResponseEntity<BookingView> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateBooking input) {
        var outcome = service.create(userId(jwt), input);
        var b = outcome.booking();
        int status =
                switch (b.status()) {
                    case CONFIRMED -> outcome.created() ? 201 : 200;
                    case PENDING -> 202;
                    case CANCELLED ->
                            throw new ApiException(
                                    HttpStatus.valueOf(
                                            b.failureStatus() == null ? 409 : b.failureStatus()),
                                    b.failureReason() == null
                                            ? "Booking cancelled"
                                            : b.failureReason(),
                                    b.id());
                };
        return ResponseEntity.status(status).body(b);
    }

    @GetMapping({"/api/bookings", "/bookings"})
    public PageDto<BookingView> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactions.history(userId(jwt), page, size);
    }

    @GetMapping({"/api/bookings/{id}", "/booking/{id}"})
    public BookingView get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return transactions.get(id, userId(jwt));
    }

    @DeleteMapping({"/api/bookings/{id}", "/booking/{id}"})
    public ResponseEntity<BookingView> cancel(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var b = service.cancel(userId(jwt), id);
        return ResponseEntity.status(b.compensationRequired() ? 202 : 200).body(b);
    }

    @GetMapping("/api/bookings/recommendations")
    public List<RoomView> recommend(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) Long hotelId) {
        return service.recommend(hotelId, startDate, endDate);
    }

    private Long userId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid user identity");
        }
    }
}
