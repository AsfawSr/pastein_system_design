package com.asfaw.pastebin.ratelimit;

import java.util.function.LongSupplier;

public final class TokenBucket {

    private final int capacity;
    private final double refillPerNano;
    private final LongSupplier nanoTime;

    private double tokens;
    private long lastRefill;

    public TokenBucket(int capacity, double refillPerMinute, LongSupplier nanoTime) {
        this.capacity = capacity;
        this.refillPerNano = refillPerMinute / 60_000_000_000.0;
        this.nanoTime = nanoTime;
        this.tokens = capacity;
        this.lastRefill = nanoTime.getAsLong();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = nanoTime.getAsLong();
        tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerNano);
        lastRefill = now;
    }
}
