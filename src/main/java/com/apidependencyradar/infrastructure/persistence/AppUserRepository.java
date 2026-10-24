
package com.apidependencyradar.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository
        extends JpaRepository<AppUserEntity, String> {

    Optional<AppUserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUserEntity u where lower(u.email) = lower(:email)")
    Optional<AppUserEntity> findByEmailForUpdate(
            @Param("email") String email);
}
