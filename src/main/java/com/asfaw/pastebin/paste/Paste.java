package com.asfaw.pastebin.paste;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
}
