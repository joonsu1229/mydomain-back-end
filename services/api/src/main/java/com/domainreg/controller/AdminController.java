package com.domainreg.controller;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.entity.PlatformDomain;
import com.domainreg.core.entity.User;
import com.domainreg.security.UserPrincipal;
import com.domainreg.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserDetail(id));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Map<String, String>> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getUserId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot change own role"));
        }
        adminService.updateUserRole(id, body.get("role"));
        return ResponseEntity.ok(Map.of("message", "Role updated"));
    }

    @PutMapping("/users/{id}/permissions")
    public ResponseEntity<Map<String, String>> updatePermissions(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        adminService.updatePermissions(id,
            body.getOrDefault("nsEnabled", false),
            body.getOrDefault("privacyEnabled", false));
        return ResponseEntity.ok(Map.of("message", "권한이 변경되었습니다."));
    }

    @PutMapping("/users/{id}/account")
    public ResponseEntity<Map<String, String>> updateUserAccount(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        adminService.updateUserAccount(id, body.get("email"), body.get("password"));
        return ResponseEntity.ok(Map.of("message", "계정 정보가 변경되었습니다."));
    }

    @PutMapping("/users/{id}/domain-limit")
    public ResponseEntity<Map<String, String>> updateDomainLimit(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        Integer limit = body.get("domainLimit");
        if (limit == null || limit < 1) {
            return ResponseEntity.badRequest().body(Map.of("message", "유효한 발급 제한 값을 입력해주세요."));
        }
        adminService.updateDomainLimit(id, limit);
        return ResponseEntity.ok(Map.of("message", "도메인 발급 제한이 변경되었습니다."));
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<Map<String, String>> suspendUser(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        adminService.suspendUser(id, body.getOrDefault("suspended", false));
        return ResponseEntity.ok(Map.of("message", "계정 상태가 변경되었습니다."));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "사용자가 삭제되었습니다."));
    }

    @GetMapping("/domains")
    public ResponseEntity<List<Domain>> listDomains() {
        return ResponseEntity.ok(adminService.getAllDomains());
    }

    @GetMapping("/domains/{id}")
    public ResponseEntity<Map<String, Object>> getDomainDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getDomainDetail(id));
    }

    @PostMapping("/domains/{id}/renew")
    public ResponseEntity<Domain> renewDomain(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.renewDomain(id));
    }

    @DeleteMapping("/domains/{id}")
    public ResponseEntity<Map<String, String>> deleteDomain(@PathVariable Long id) {
        adminService.deleteDomain(id);
        return ResponseEntity.ok(Map.of("message", "도메인이 삭제되었습니다."));
    }
}
