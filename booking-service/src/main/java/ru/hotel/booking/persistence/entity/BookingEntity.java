package ru.hotel.booking.persistence.entity;

import jakarta.persistence.*;
import ru.hotel.booking.domain.Role;
import ru.hotel.booking.domain.BookingStatus;
import java.time.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
        name = "bookings",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_booking_request",
                        columnNames = {"user_id", "request_id"}),
        indexes = {
            @Index(name = "idx_booking_owner", columnList = "user_id,created_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class BookingEntity {
    @Id private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(nullable = false)
    private String fingerprint;

    private Long roomId;
    private Long hotelId;

    @Column(nullable = false)
    private boolean autoSelect;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "compensation_required", nullable = false)
    private boolean compensationRequired;

    private String failureReason;
    private Integer failureStatus;
    @Version private long version;
}
