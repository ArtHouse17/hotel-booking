package ru.hotel.booking.persistence.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;

import ru.hotel.booking.persistence.entity.UserEntity;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> lockById(Long id);
}
