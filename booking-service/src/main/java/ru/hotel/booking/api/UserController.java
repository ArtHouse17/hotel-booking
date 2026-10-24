package ru.hotel.booking.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.hotel.booking.api.UserDtos.*;
import ru.hotel.booking.service.UserService;
import ru.hotel.common.PageDto;

@RestController
@RequestMapping({"/api/users", "/user"})
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public PageDto<UserView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return users.list(page, size);
    }

    @GetMapping("/{id}")
    public UserView get(@PathVariable Long id) {
        return users.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody UserInput input) {
        return users.create(input);
    }

    @PatchMapping("/{id}")
    public UserView patch(@PathVariable Long id, @Valid @RequestBody UserPatch input) {
        return users.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        users.delete(id);
    }
}
