package ru.hotel.booking.domain;

import lombok.*;

@Getter @Setter @NoArgsConstructor
public class User {
    private Long id;
    private String username;
    private String password;
    private Role role;

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
