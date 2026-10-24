package ru.hotel.booking.domain.repository;

import ru.hotel.booking.domain.User;
import ru.hotel.common.PageDto;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);

    Optional<User> lockById(Long id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    PageDto<User> findAll(int page, int size);

    User save(User user);

    void delete(User user);
}
