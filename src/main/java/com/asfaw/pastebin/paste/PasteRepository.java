package com.asfaw.pastebin.paste;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // atomic in-database increment: safe under concurrent reads, no lost updates
    @Modifying
    @Query("update Paste p set p.views = p.views + 1 where p.id = :id")
    int incrementViews(@Param("id") String id);

    @Query("""
            select p from Paste p
            where p.visibility = :visibility
              and (p.expiresAt is null or p.expiresAt > :now)
              and p.burnAfterRead = false
            order by p.createdAt desc
            """)
    Page<Paste> findVisible(@Param("visibility") PasteVisibility visibility,
                            @Param("now") Instant now,
                            Pageable pageable);

    @Query("select p from Paste p where p.owner.username = :username order by p.createdAt desc")
    Page<Paste> findByOwnerUsername(@Param("username") String username, Pageable pageable);
}
