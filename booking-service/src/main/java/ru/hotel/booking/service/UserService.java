package ru.hotel.booking.service;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.booking.api.*;
import ru.hotel.booking.api.UserDtos.*;
import ru.hotel.booking.domain.*;
import ru.hotel.booking.repository.*;
import ru.hotel.common.*;
import ru.hotel.common.security.JwtSupport;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final BookingRepository bookings;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final BookingMapper mapper;

    public UserService(
            UserRepository users,
            BookingRepository bookings,
            PasswordEncoder passwords,
            JwtEncoder encoder,
            BookingMapper mapper) {
        this.users = users;
        this.bookings = bookings;
        this.passwords = passwords;
        this.encoder = encoder;
        this.mapper = mapper;
    }

    @Transactional
    public TokenView register(Credentials input) {
        return token(create(input.username(), input.password(), Role.USER));
    }

    public TokenView login(Credentials input) {
        var user =
                users.findByUsername(input.username())
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwords.matches(input.password(), user.getPassword()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        return token(user);
    }

    public PageDto<UserView> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw ApiException.badRequest("page >= 0; size must be 1..100");
        var result = users.findAll(PageRequest.of(page, size, Sort.by("id")));
        return new PageDto<>(
                result.map(mapper::user).getContent(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages());
    }

    public UserView get(Long id) {
        return mapper.user(find(id));
    }

    @Transactional
    public UserView create(UserInput input) {
        return mapper.user(create(input.username(), input.password(), input.role()));
    }

    @Transactional
    public UserView update(Long id, UserPatch input) {
        var user = users.lockById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (input.username() != null) user.setUsername(input.username());
        if (input.password() != null) user.setPassword(passwords.encode(input.password()));
        if (input.role() != null) user.setRole(input.role());
        return mapper.user(user);
    }

    @Transactional
    public void delete(Long id) {
        var user = users.lockById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (bookings.existsByUserId(id)) throw ApiException.conflict("User has booking history");
        users.delete(user);
    }

    private User create(String username, String password, Role role) {
        if (users.existsByUsername(username))
            throw ApiException.conflict("Username is already taken");
        return users.save(new User(username, passwords.encode(password), role));
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private TokenView token(User user) {
        return new TokenView(
                JwtSupport.token(encoder, user.getId().toString(), user.getRole().name(), 3600),
                "Bearer",
                3600,
                mapper.user(user));
    }
}
