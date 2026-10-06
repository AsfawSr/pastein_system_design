package com.asfaw.pastebin.paste;

import java.time.Duration;

public record CreatePasteCommand(
        String title,
        String content,
        Duration ttl,
        boolean burnAfterRead,
        String password,
        PasteVisibility visibility,
        String language) {
}
