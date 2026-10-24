package ru.hotel.booking.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.hotel.booking.api.UserDtos.*;
import ru.hotel.booking.service.UserService;

@RestController
public class AuthController {
    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    @PostMapping({"/api/auth/register", "/user/register"})
    @ResponseStatus(HttpStatus.CREATED)
    public TokenView register(@Valid @RequestBody Credentials input) {
        return users.register(input);
    }

    @PostMapping({"/api/auth/login", "/user/auth"})
    public TokenView login(@Valid @RequestBody Credentials input) {
        return users.login(input);
    }
}
