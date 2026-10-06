package com.asfaw.pastebin.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "pastebin.rate-limit")
public record RateLimitProperties(int capacity, Duration window) {
}
