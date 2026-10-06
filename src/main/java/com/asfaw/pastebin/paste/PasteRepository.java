package com.asfaw.pastebin.paste;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasteRepository extends JpaRepository<Paste, String> {

    @Modifying
    @Query("delete from Paste p where p.expiresAt is not null and p.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Paste p where p.id = :id")
    Optional<Paste> findByIdForUpdate(@Param("id") String id);
}
