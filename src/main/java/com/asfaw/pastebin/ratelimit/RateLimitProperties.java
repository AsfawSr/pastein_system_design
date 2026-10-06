package com.asfaw.pastebin.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pastebin.rate-limit")
public record RateLimitProperties(int capacity, double refillPerMinute) {
}
