package com.asfaw.pastebin.apikey;

// what a validated key grants: identity plus its own rate-limit budget
public record ApiKeyAuth(Long keyId, String username, int rateLimitPerMinute) {
}
