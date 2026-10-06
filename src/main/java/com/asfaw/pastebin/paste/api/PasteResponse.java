package com.asfaw.pastebin.paste.api;

import com.asfaw.pastebin.paste.Paste;
import com.asfaw.pastebin.paste.PasteVisibility;

import java.time.Instant;

public record PasteResponse(
        String id,
        String title,
        String content,
        String language,
        Instant createdAt,
        Instant expiresAt,
        long views,
        PasteVisibility visibility,
        boolean burned) {

    public static PasteResponse from(Paste paste, boolean burned) {
        return new PasteResponse(
                paste.getId(),
                paste.getTitle(),
                paste.getContent(),
                paste.getLanguage(),
                paste.getCreatedAt(),
                paste.getExpiresAt(),
                paste.getViews(),
                paste.getVisibility(),
                burned);
    }
}
