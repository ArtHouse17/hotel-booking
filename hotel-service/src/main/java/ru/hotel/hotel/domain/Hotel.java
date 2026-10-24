package ru.hotel.hotel.domain;

import lombok.*;

@Getter @Setter @NoArgsConstructor
public class Hotel {
    private Long id;
    private String name;
    private String address;

    public Hotel(String name, String address) {
        this.name = name;
        this.address = address;
    }
}
