package ru.hotel.booking.persistence;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.hotel.booking.domain.User;
import ru.hotel.booking.domain.repository.UserRepository;
import ru.hotel.booking.persistence.entity.UserEntity;
import ru.hotel.booking.persistence.repository.UserJpaRepository;
import ru.hotel.common.PageDto;

import java.util.*;

@Repository
@Transactional(readOnly = true)
public class JpaUserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository jpa;
    private final BookingPersistenceMapper mapper;

    public JpaUserRepositoryAdapter(UserJpaRepository jpa, BookingPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(mapper::domain);
    }

    @Transactional
    public Optional<User> lockById(Long id) {
        return jpa.lockById(id).map(mapper::domain);
    }

    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(mapper::domain);
    }

    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }

    public PageDto<User> findAll(int page, int size) {
        var result = jpa.findAll(PageRequest.of(page, size, Sort.by("id")));
        return new PageDto<>(
                result.map(mapper::domain).getContent(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public User save(User user) {
        var entity =
                user.getId() == null
                        ? new UserEntity()
                        : jpa.findById(user.getId()).orElseGet(UserEntity::new);
        mapper.update(user, entity);
        return mapper.domain(jpa.save(entity));
    }

    @Transactional
    public void delete(User user) {
        jpa.deleteById(user.getId());
    }
}
