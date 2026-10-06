package com.asfaw.pastebin.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketTest {

    private long fakeNanos = 0;

    @Test
    void allowsUpToCapacityImmediately() {
        TokenBucket bucket = new TokenBucket(3, 60, () -> fakeNanos);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    void refillsOverTime() {
        TokenBucket bucket = new TokenBucket(1, 60, () -> fakeNanos); // 1 token per second

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();

        fakeNanos += 1_000_000_000L; // +1s
        assertThat(bucket.tryConsume()).isTrue();
    }

    @Test
    void neverExceedsCapacityAfterLongIdle() {
        TokenBucket bucket = new TokenBucket(2, 60, () -> fakeNanos);

        fakeNanos += 3_600_000_000_000L; // +1h idle
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }
}
