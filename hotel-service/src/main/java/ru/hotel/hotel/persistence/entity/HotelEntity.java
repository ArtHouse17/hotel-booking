package ru.hotel.hotel.persistence.entity;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "hotels")
@Getter
@Setter
@NoArgsConstructor
public class HotelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 300)
    private String address;

    public HotelEntity(String name, String address) {
        this.name = name;
        this.address = address;
    }
}
