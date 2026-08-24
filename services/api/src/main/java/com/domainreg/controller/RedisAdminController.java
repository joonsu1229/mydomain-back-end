package com.domainreg.controller;

import com.domainreg.service.RedisAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class RedisAdminController {

    private final RedisAdminService redisAdminService;

    public RedisAdminController(RedisAdminService redisAdminService) {
        this.redisAdminService = redisAdminService;
    }

    // ── Server info ──
    @GetMapping("/redis/info")
    public ResponseEntity<Map<String, Object>> getServerInfo() {
        return ResponseEntity.ok(redisAdminService.getServerInfo());
    }

    // ── Stats summary ──
    @GetMapping("/redis/stats")
    public ResponseEntity<Map<String, Object>> getRedisStats() {
        return ResponseEntity.ok(redisAdminService.getRedisStats());
    }

    // ── Key search ──
    @GetMapping("/redis/keys")
    public ResponseEntity<?> searchKeys(@RequestParam(defaultValue = "") String pattern) {
        return ResponseEntity.ok(redisAdminService.searchKeys(pattern));
    }

    // ── Key detail ──
    @GetMapping("/redis/keys/{key}")
    public ResponseEntity<Map<String, Object>> getKeyDetail(@PathVariable String key) {
        return ResponseEntity.ok(redisAdminService.getKeyDetail(key));
    }

    // ── Delete individual key ──
    @DeleteMapping("/redis/keys/{key}")
    public ResponseEntity<Map<String, Object>> deleteKey(@PathVariable String key) {
        return ResponseEntity.ok(redisAdminService.deleteKey(key));
    }

    // ── Flush all ──
    @DeleteMapping("/redis/flush")
    public ResponseEntity<Map<String, Object>> flushAll() {
        return ResponseEntity.ok(redisAdminService.flushAll());
    }

    // ── Existing: delete refresh token ──
    @DeleteMapping("/redis/refresh/{userId}")
    public ResponseEntity<Map<String, Object>> deleteRefreshToken(@PathVariable Long userId) {
        return ResponseEntity.ok(redisAdminService.deleteRefreshToken(userId));
    }

    // ── Existing: delete verification token ──
    @DeleteMapping("/redis/verify/{fullToken}")
    public ResponseEntity<Map<String, Object>> deleteVerificationToken(@PathVariable String fullToken) {
        return ResponseEntity.ok(redisAdminService.deleteVerificationToken(fullToken));
    }

    // ── Resend verification email ──
    @PostMapping("/redis/verify/resend")
    public ResponseEntity<Map<String, Object>> resendVerificationEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("resent", false, "message", "이메일을 입력해주세요."));
        }
        return ResponseEntity.ok(redisAdminService.resendVerificationEmail(email));
    }

    // ── Existing: clear rate limits ──
    @DeleteMapping("/redis/rate-limits")
    public ResponseEntity<Map<String, Object>> clearRateLimits() {
        return ResponseEntity.ok(redisAdminService.clearRateLimits());
    }
}
