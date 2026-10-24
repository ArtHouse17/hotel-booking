package ru.hotel.booking.api;

import jakarta.validation.constraints.*;
import ru.hotel.booking.domain.Role;

public final class UserDtos {
    private UserDtos() {}

    public record Credentials(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_.-]{3,60}") String username,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record UserInput(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_.-]{3,60}") String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull Role role) {}

    public record UserPatch(
            @Pattern(regexp = "[a-zA-Z0-9_.-]{3,60}") String username,
            @Size(min = 8, max = 72) String password,
            Role role) {}

    public record UserView(Long id, String username, Role role) {}

    public record TokenView(String token, String tokenType, long expiresIn, UserView user) {}
}
