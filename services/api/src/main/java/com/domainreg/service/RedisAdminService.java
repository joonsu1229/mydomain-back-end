package com.domainreg.service;

import com.domainreg.core.entity.User;
import com.domainreg.persistence.mapper.UserMapper;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class RedisAdminService {

    private final StringRedisTemplate redis;
    private final UserMapper userMapper;
    private final EmailService emailService;

    public RedisAdminService(StringRedisTemplate redis, UserMapper userMapper, EmailService emailService) {
        this.redis = redis;
        this.userMapper = userMapper;
        this.emailService = emailService;
    }

    // ──────────────────────────────────────────────
    // 1. Server info
    // ──────────────────────────────────────────────
    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // Redis server properties
        try {
            Properties props = redis.getRequiredConnectionFactory()
                .getConnection()
                .info();
            info.put("redisVersion", props.getProperty("redis_version", "unknown"));
            info.put("uptimeInDays", Long.parseLong(props.getProperty("uptime_in_seconds", "0")) / 86400L);
            info.put("connectedClients", Integer.parseInt(props.getProperty("connected_clients", "0")));
            info.put("usedMemoryHuman", props.getProperty("used_memory_human", "0"));
            info.put("usedMemoryBytes", Long.parseLong(props.getProperty("used_memory", "0")));
            info.put("maxMemoryHuman", props.getProperty("maxmemory_human", "unlimited"));
            info.put("maxMemoryBytes", props.getProperty("maxmemory").equals("0") ? -1L
                : Long.parseLong(props.getProperty("maxmemory", "0")));
            info.put("keyspaceHits", Long.parseLong(props.getProperty("keyspace_hits", "0")));
            info.put("keyspaceMisses", Long.parseLong(props.getProperty("keyspace_misses", "0")));
            info.put("totalKeys", redis.keys("*").size());
        } catch (Exception e) {
            info.put("error", "Redis INFO를 가져올 수 없습니다: " + e.getMessage());
        }
        return info;
    }

    // ──────────────────────────────────────────────
    // 2. Stats summary (existing + enhanced)
    // ──────────────────────────────────────────────
    public Map<String, Object> getRedisStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Refresh tokens (logged-in users)
        Set<String> refreshKeys = redis.keys("refresh:*");
        List<Map<String, Object>> loggedInUsers = new ArrayList<>();
        if (refreshKeys != null) {
            for (String key : refreshKeys) {
                String userIdStr = key.substring("refresh:".length());
                Long userId = Long.parseLong(userIdStr);
                Long ttl = redis.getExpire(key);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("userId", userId);
                userMapper.findById(userId).ifPresentOrElse(
                    u -> { entry.put("loginId", u.getLoginId()); entry.put("email", u.getEmail()); },
                    () -> { entry.put("loginId", "(알 수 없음)"); entry.put("email", ""); }
                );
                entry.put("ttlSeconds", ttl != null ? ttl : -1);
                loggedInUsers.add(entry);
            }
        }
        stats.put("loggedInCount", loggedInUsers.size());
        stats.put("loggedInUsers", loggedInUsers);

        // Pending email verifications
        Set<String> verifyKeys = redis.keys("verify_token:*");
        List<Map<String, Object>> pendingVerifications = new ArrayList<>();
        if (verifyKeys != null) {
            for (String key : verifyKeys) {
                String token = key.substring("verify_token:".length());
                String email = redis.opsForValue().get(key);
                Long ttl = redis.getExpire(key);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("token", token.length() > 12 ? token.substring(0, 12) + "..." : token);
                entry.put("fullToken", token);
                entry.put("email", email);
                entry.put("ttlSeconds", ttl != null ? ttl : -1);
                pendingVerifications.add(entry);
            }
        }
        stats.put("pendingVerificationCount", pendingVerifications.size());
        stats.put("pendingVerifications", pendingVerifications);

        // Role cache
        Set<String> roleKeys = redis.keys("role:*");
        stats.put("roleCacheCount", roleKeys != null ? roleKeys.size() : 0);

        // Rate limit keys
        Set<String> rlKeys = redis.keys("rl:*");
        stats.put("rateLimitKeyCount", rlKeys != null ? rlKeys.size() : 0);

        // Revoked access tokens
        Set<String> revokedKeys = redis.keys("revoked:*");
        stats.put("revokedCount", revokedKeys != null ? revokedKeys.size() : 0);

        // Key breakdown by prefix
        Map<String, Integer> keyBreakdown = new LinkedHashMap<>();
        Set<String> allKeys = redis.keys("*");
        if (allKeys != null) {
            for (String k : allKeys) {
                String prefix = k.contains(":") ? k.substring(0, k.indexOf(':')) : "(other)";
                keyBreakdown.merge(prefix, 1, Integer::sum);
            }
        }
        stats.put("keyBreakdown", keyBreakdown);

        return stats;
    }

    // ──────────────────────────────────────────────
    // 3. Key search
    // ──────────────────────────────────────────────
    public List<Map<String, Object>> searchKeys(String pattern) {
        List<Map<String, Object>> results = new ArrayList<>();
        String searchPattern = pattern != null && !pattern.isBlank() ? "*" + pattern + "*" : "*";
        Set<String> keys = redis.keys(searchPattern);
        if (keys == null) return results;

        for (String key : keys) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("key", key);
            entry.put("type", redis.type(key) != null ? redis.type(key).code() : "unknown");
            Long ttl = redis.getExpire(key);
            entry.put("ttlSeconds", ttl != null ? ttl : -1);
            // Value preview (truncated)
            String value = null;
            try {
                value = redis.opsForValue().get(key);
            } catch (Exception ignored) {}
            if (value == null) value = "(binary/hash/list/set)";
            entry.put("valuePreview", value.length() > 80 ? value.substring(0, 80) + "..." : value);
            results.add(entry);
        }
        // Sort by key name
        results.sort(Comparator.comparing(m -> m.get("key").toString()));
        // Limit to 200
        if (results.size() > 200) results = results.subList(0, 200);
        return results;
    }

    // ──────────────────────────────────────────────
    // 4. Key detail
    // ──────────────────────────────────────────────
    public Map<String, Object> getKeyDetail(String key) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("key", key);
        Long ttl = redis.getExpire(key);
        detail.put("ttlSeconds", ttl != null ? ttl : -1);
        String type = redis.type(key) != null ? redis.type(key).code() : "none";
        detail.put("type", type);

        if ("string".equals(type)) {
            String value = redis.opsForValue().get(key);
            detail.put("value", value);
            detail.put("valueLength", value != null ? value.length() : 0);
        } else if ("hash".equals(type)) {
            Map<Object, Object> entries = redis.opsForHash().entries(key);
            Map<String, String> flat = new LinkedHashMap<>();
            entries.forEach((k, v) -> flat.put(String.valueOf(k), String.valueOf(v)));
            detail.put("value", flat);
            detail.put("fieldCount", entries.size());
        } else if ("list".equals(type)) {
            Long size = redis.opsForList().size(key);
            detail.put("value", "(list, " + size + " items)");
            detail.put("listSize", size);
        } else if ("set".equals(type)) {
            Long size = redis.opsForSet().size(key);
            detail.put("value", "(set, " + size + " members)");
            detail.put("setSize", size);
        } else {
            detail.put("value", "(empty or unsupported type)");
        }

        return detail;
    }

    // ──────────────────────────────────────────────
    // 5. Delete individual key
    // ──────────────────────────────────────────────
    public Map<String, Object> deleteKey(String key) {
        Boolean deleted = redis.delete(key);
        return Map.of("deleted", deleted, "message",
            deleted ? "키 '" + key + "' 삭제 완료" : "키 '" + key + "' 를 찾을 수 없습니다.");
    }

    // ──────────────────────────────────────────────
    // 6. Flush all (dangerous!)
    // ──────────────────────────────────────────────
    public Map<String, Object> flushAll() {
        try {
            redis.getRequiredConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
            return Map.of("success", true, "message", "Redis 전체 데이터가 삭제되었습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Flush 실패: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════
    // Existing mutation methods
    // ══════════════════════════════════════════════

    public Map<String, Object> deleteRefreshToken(Long userId) {
        Boolean deleted = redis.delete("refresh:" + userId);
        redis.opsForValue().set("revoked:" + userId, "1", Duration.ofMinutes(15));
        return Map.of("deleted", deleted, "message",
            deleted ? "강제 로그아웃 처리되었습니다." : "세션은 없었지만 액세스 토큰을 차단했습니다.");
    }

    public Map<String, Object> deleteVerificationToken(String fullToken) {
        Boolean deleted = redis.delete("verify_token:" + fullToken);
        return Map.of("deleted", deleted, "message",
            deleted ? "인증 토큰이 만료되었습니다." : "해당 인증 토큰을 찾을 수 없습니다.");
    }

    public Map<String, Object> resendVerificationEmail(String email) {
        Optional<User> userOpt = userMapper.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Map.of("resent", false, "message", "해당 이메일로 등록된 사용자를 찾을 수 없습니다.");
        }
        User user = userOpt.get();

        if (user.isEmailVerified()) {
            return Map.of("resent", false, "message", "이미 인증이 완료된 계정입니다.");
        }

        String oldToken = user.getVerificationToken();
        String newToken = UUID.randomUUID().toString().replace("-", "");

        // DB의 인증 토큰을 새 토큰으로 교체
        userMapper.updateVerificationToken(user.getId(), newToken);

        // 기존 Redis verify 토큰 제거 (새 키는 sendVerificationEmail 내부에서 생성)
        if (oldToken != null && !oldToken.isBlank()) {
            redis.delete("verify_token:" + oldToken);
        }

        // 인증 메일 재발송
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), newToken);

        return Map.of("resent", true, "message", user.getEmail() + " 인증 메일을 재발송했습니다.");
    }

    public Map<String, Object> clearRateLimits() {
        Set<String> keys = redis.keys("rl:*");
        long deleted = 0;
        if (keys != null && !keys.isEmpty()) {
            deleted = redis.delete(keys);
        }
        return Map.of("deleted", deleted, "message", "Rate limit keys cleared");
    }
}
