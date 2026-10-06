package com.asfaw.pastebin.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Clock;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        long windowSeconds = properties.window().toSeconds();
        long windowIndex = clock.instant().getEpochSecond() / windowSeconds;
        String key = "ratelimit:" + request.getRemoteAddr() + ":" + windowIndex;

        // INCR is atomic across all app instances sharing this Redis
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, properties.window().multipliedBy(2));
        }
        if (count == null || count <= properties.capacity()) {
            return true;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(windowSeconds));
        response.setContentType("text/plain");
        response.getWriter().write("Too many pastes created, please slow down.");
        return false;
    }
}
