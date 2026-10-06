package com.asfaw.pastebin.paste;

import com.asfaw.pastebin.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "pastes")
@Getter
@Setter
@NoArgsConstructor
public class Paste {

    @Id
    @Column(length = 16)
    private String id;

    @Column(length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant expiresAt;

    @Column(nullable = false)
    private boolean burnAfterRead;

    @Column(nullable = false)
    private long views;

    @Column(length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PasteVisibility visibility = PasteVisibility.UNLISTED;

    @Column(nullable = false, length = 32)
    private String language = "plaintext";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
}
