package com.domainreg.security;

import com.domainreg.persistence.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final StringRedisTemplate redis;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UserMapper userMapper,
                                   StringRedisTemplate redis) {
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && tokenProvider.isTokenValid(token)) {
            Claims claims = tokenProvider.validate(token);
            Long userId = tokenProvider.getUserId(claims);
            String loginId = claims.get("loginId", String.class);
            String email = claims.get("email", String.class);

            // Check if user was forcibly logged out by admin
            if (Boolean.TRUE.equals(redis.hasKey("revoked:" + userId))) {
                // Access token revoked — deny this request
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Look up role: Redis cache → DB fallback → JWT fallback
            String role = lookupRole(userId, claims);

            var principal = new UserPrincipal(userId, loginId, email, role);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String lookupRole(Long userId, Claims claims) {
        // 1. Try Redis cache
        String cached = redis.opsForValue().get("role:" + userId);
        if (cached != null) {
            return cached;
        }
        // 2. Try DB (1h cache to avoid repeated hits)
        String role = userMapper.findById(userId)
            .map(u -> u.getRole())
            .orElse(claims.get("role", String.class));
        redis.opsForValue().set("role:" + userId, role, Duration.ofHours(1));
        return role;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
