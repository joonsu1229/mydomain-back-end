package com.domainreg.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.domainreg.core.entity.User;
import com.domainreg.dto.*;
import com.domainreg.security.UserPrincipal;
import com.domainreg.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.loginId(), request.email(), request.password(),
            request.name(), request.phone());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "인증 이메일을 발송했습니다. 이메일을 확인해주세요.",
            "loginId", user.getLoginId(),
            "email", user.getEmail()
        ));
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestParam("token") String token) {
        User user = authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of(
            "message", "이메일 인증이 완료되었습니다. 이제 로그인할 수 있습니다.",
            "loginId", user.getLoginId(),
            "email", user.getEmail()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        var token = authService.login(request.loginId(), request.password(), ip);
        return ResponseEntity.ok(new AuthResponse(
            token.accessToken(), token.refreshToken(), 900));
    }

    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam("value") String value) {
        boolean available = !authService.loginIdExists(value);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam("value") String email) {
        boolean available = !authService.emailExists(email);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        var token = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(new AuthResponse(
            token.accessToken(), token.refreshToken(), 900));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UserPrincipal principal) {
        User user = authService.getUser(principal.getUserId());
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "loginId", user.getLoginId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "phone", user.getPhone() != null ? user.getPhone() : "",
            "role", user.getRole(),
            "nsEnabled", user.isNsEnabled(),
            "privacyEnabled", user.isPrivacyEnabled(),
            "createdAt", user.getCreatedAt().toString()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of(
            "message", "비밀번호 재설정 링크를 이메일로 발송했습니다. 이메일을 확인해주세요."
        ));
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<Map<String, Object>> validateResetToken(@RequestParam("token") String token) {
        boolean valid = authService.validateResetToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok(Map.of(
            "message", "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요."
        ));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
