package com.domainreg.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    private final StringRedisTemplate redis;
    private final Map<String, RateLimitConfig> limits = new ConcurrentHashMap<>();

    public RateLimitFilter(StringRedisTemplate redis) {
        this.redis = redis;
        limits.put("/api/auth/login", new RateLimitConfig(5, Duration.ofMinutes(5)));
        limits.put("/api/auth/register", new RateLimitConfig(3, Duration.ofMinutes(10)));
        limits.put("/api/domains/search", new RateLimitConfig(30, Duration.ofMinutes(1)));
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();
        RateLimitConfig cfg = limits.get(path);

        if (cfg == null) {
            chain.doFilter(req, res);
            return;
        }

        String ip = getClientIp(request);
        String key = "rl:" + path + ":" + ip;

        Long count = redis.opsForValue().increment(key);
        if (count == 1) {
            redis.expire(key, cfg.window());
        }

        if (count != null && count > cfg.maxRequests()) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        chain.doFilter(req, res);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record RateLimitConfig(int maxRequests, Duration window) {}
}
