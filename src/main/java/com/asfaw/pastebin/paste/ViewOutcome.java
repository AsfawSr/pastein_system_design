package com.asfaw.pastebin.paste;

public sealed interface ViewOutcome {

    record Viewed(Paste paste, boolean burned, String ownerUsername) implements ViewOutcome {
    }

    record PasswordRequired(boolean wrongAttempt) implements ViewOutcome {
    }
}
