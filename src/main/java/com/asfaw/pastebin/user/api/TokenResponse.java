package com.asfaw.pastebin.user.api;

import java.time.Instant;

public record TokenResponse(String token, Instant expiresAt) {
}
