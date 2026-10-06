package com.asfaw.pastebin.paste;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface PasteRepository extends JpaRepository<Paste, String> {

    @Modifying
    @Query("delete from Paste p where p.expiresAt is not null and p.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
