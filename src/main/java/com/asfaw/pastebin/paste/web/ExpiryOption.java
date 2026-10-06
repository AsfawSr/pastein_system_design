package com.asfaw.pastebin.paste.web;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum ExpiryOption {

    NEVER("Never", null),
    TEN_MINUTES("10 minutes", Duration.ofMinutes(10)),
    ONE_HOUR("1 hour", Duration.ofHours(1)),
    ONE_DAY("1 day", Duration.ofDays(1)),
    ONE_WEEK("1 week", Duration.ofDays(7));

    private final String label;
    private final Duration duration;
}
