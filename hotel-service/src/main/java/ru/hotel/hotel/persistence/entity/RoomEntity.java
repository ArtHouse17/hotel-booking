package ru.hotel.hotel.persistence.entity;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(columnNames = {"hotel_id", "number"}))
@Getter
@Setter
@NoArgsConstructor
public class RoomEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private HotelEntity hotel;

    @Column(nullable = false, length = 30)
    private String number;

    @Column(nullable = false)
    private boolean available = true;

    @Column(name = "times_booked", nullable = false)
    private long timesBooked;

    @Version private long version;

    public RoomEntity(HotelEntity hotel, String number, boolean available) {
        this.hotel = hotel;
        this.number = number;
        this.available = available;
    }
}
