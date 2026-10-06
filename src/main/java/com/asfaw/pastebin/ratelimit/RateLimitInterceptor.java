package com.asfaw.pastebin.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        TokenBucket bucket = buckets.computeIfAbsent(request.getRemoteAddr(),
                ip -> new TokenBucket(properties.capacity(), properties.refillPerMinute(), System::nanoTime));
        if (bucket.tryConsume()) {
            return true;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType("text/plain");
        response.getWriter().write("Too many pastes created, please slow down.");
        return false;
    }
}
