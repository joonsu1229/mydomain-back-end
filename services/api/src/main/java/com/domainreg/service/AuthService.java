package com.domainreg.service;

import com.domainreg.core.entity.User;
import com.domainreg.core.port.UserRepository;
import com.domainreg.persistence.mapper.UserMapper;
import com.domainreg.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    // 가입 허용 이메일 도메인 (공신력 있는 이메일 제공자만 허용)
    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(
        "gmail.com",
        "naver.com",
        "daum.net",
        "hanmail.net",
        "kakao.com",
        "outlook.com",
        "hotmail.com",
        "icloud.com"
    );

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redis;
    private final EmailService emailService;
    private final TermsService termsService;

    public AuthService(UserRepository userRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       StringRedisTemplate redis,
                       EmailService emailService,
                       TermsService termsService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.redis = redis;
        this.emailService = emailService;
        this.termsService = termsService;
    }

    @Transactional
    public User register(String loginId, String email, String password, String name, String phone,
                         Long termsId, Long privacyId, String ip) {
        // Validate loginId format
        if (!loginId.matches("^[a-zA-Z0-9]+$")) {
            throw new AuthException("INVALID_USERNAME",
                "아이디는 영문자와 숫자만 사용할 수 있습니다.");
        }
        if (loginId.length() < 3 || loginId.length() > 30) {
            throw new AuthException("INVALID_USERNAME",
                "아이디는 3자 이상 30자 이하여야 합니다.");
        }

        // 이메일 도메인 검증: 공신력 있는 이메일 제공자만 허용
        if (!ALLOWED_EMAIL_DOMAINS.contains(extractDomain(email))) {
            throw new AuthException("INVALID_EMAIL_DOMAIN",
                "지원하지 않는 이메일 도메인입니다. gmail, naver, daum 등 공신력 있는 이메일만 가입할 수 있어요.");
        }

        // Check duplicate loginId
        if (userRepository.existsByLoginId(loginId)) {
            throw new AuthException("USERNAME_EXISTS", "이미 사용 중인 아이디입니다.");
        }

        // Check duplicate email
        if (userRepository.existsByEmail(email)) {
            throw new AuthException("EMAIL_EXISTS", "이미 사용 중인 이메일입니다.");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        User user = User.create(loginId, email, passwordEncoder.encode(password), name, phone);
        user.setVerificationToken(token);
        user = userRepository.save(user);

        // Record agreement to the current TERMS & PRIVACY versions
        termsService.recordAgreements(user.getId(), termsId, privacyId, ip);

        // Send verification email
        emailService.sendVerificationEmail(email, name, token);

        return user;
    }

    public User verifyEmail(String token) {
        User user = userMapper.findByVerificationToken(token)
            .orElseThrow(() -> new AuthException("INVALID_TOKEN", "유효하지 않은 인증 토큰입니다."));
        userMapper.verifyEmail(user.getId());
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        return user;
    }

    public AuthToken login(String loginId, String password, String ip) {
        User user = userRepository.findByLoginId(loginId)
            .orElseThrow(() -> new AuthException("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."));

        // Check if account is temporarily locked
        if (user.isLocked()) {
            long minutesLeft = (user.getLockedUntil().getEpochSecond() - Instant.now().getEpochSecond()) / 60 + 1;
            throw new AuthException("ACCOUNT_LOCKED",
                "계정이 잠겼습니다. " + minutesLeft + "분 후에 다시 시도해주세요.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            userRepository.recordLoginFailure(user.getId());
            int attempts = user.getFailedLoginAttempts() + 1;

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                Instant lockedUntil = Instant.now().plus(LOCK_DURATION);
                userRepository.lockAccount(user.getId(), lockedUntil);
                throw new AuthException("ACCOUNT_LOCKED",
                    "로그인 실패 " + MAX_FAILED_ATTEMPTS + "회 초과. 계정이 " +
                    LOCK_DURATION.toMinutes() + "분 동안 잠겼습니다.");
            }

            int remaining = MAX_FAILED_ATTEMPTS - attempts;
            throw new AuthException("INVALID_CREDENTIALS",
                "아이디 또는 비밀번호가 올바르지 않습니다. (남은 시도: " + remaining + "회)");
        }

        if (!user.isEmailVerified()) {
            throw new AuthException("EMAIL_NOT_VERIFIED", "이메일 인증이 완료되지 않았습니다. 이메일을 확인해주세요.");
        }

        userRepository.recordLoginSuccess(user.getId(), ip);
        return issueTokens(user);
    }

    public AuthToken refresh(String refreshToken) {
        if (!tokenProvider.isTokenValid(refreshToken)) {
            throw new AuthException("TOKEN_EXPIRED", "Refresh token expired.");
        }
        Claims claims = tokenProvider.validate(refreshToken);
        Long userId = tokenProvider.getUserId(claims);

        String stored = redis.opsForValue().get("refresh:" + userId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new AuthException("TOKEN_REVOKED", "Refresh token revoked.");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "User not found."));

        redis.delete("refresh:" + userId);
        return issueTokens(user);
    }

    public void logout(Long userId) {
        redis.delete("refresh:" + userId);
    }

    // ═══════════════════════════════════════════
    // Password reset
    // ═══════════════════════════════════════════
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthException("USER_NOT_FOUND",
                "해당 이메일로 등록된 계정을 찾을 수 없습니다."));

        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("reset_token:" + token, user.getId().toString(), Duration.ofMinutes(15));
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
    }

    public boolean validateResetToken(String token) {
        String userIdStr = redis.opsForValue().get("reset_token:" + token);
        return userIdStr != null;
    }

    public void resetPassword(String token, String newPassword) {
        String userIdStr = redis.opsForValue().get("reset_token:" + token);
        if (userIdStr == null) {
            throw new AuthException("INVALID_TOKEN", "만료되었거나 유효하지 않은 재설정 링크입니다.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new AuthException("WEAK_PASSWORD", "비밀번호는 6자 이상이어야 합니다.");
        }

        Long userId = Long.parseLong(userIdStr);
        String encoded = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(userId, encoded);

        // Delete the token so it can't be reused
        redis.delete("reset_token:" + token);
        // Invalidate all existing sessions (force re-login)
        redis.delete("refresh:" + userId);
    }

    private AuthToken issueTokens(User user) {
        String accessToken = tokenProvider.createAccessToken(
            user.getId(), user.getLoginId(), user.getEmail(), user.getRole());
        String refreshToken = tokenProvider.createRefreshToken(user.getId());

        redis.opsForValue().set(
            "refresh:" + user.getId(),
            refreshToken,
            Duration.ofDays(7)
        );

        return new AuthToken(accessToken, refreshToken);
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
    }

    public boolean loginIdExists(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    private static String extractDomain(String email) {
        int at = email.indexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return "";
        }
        return email.substring(at + 1).trim().toLowerCase(Locale.ROOT);
    }

    public record AuthToken(String accessToken, String refreshToken) {}

    public static class AuthException extends RuntimeException {
        private final String code;
        public AuthException(String code, String message) { super(message); this.code = code; }
        public String getCode() { return code; }
    }
}
