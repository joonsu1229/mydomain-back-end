package com.domainreg.controller;

import com.domainreg.core.entity.AppSetting;
import com.domainreg.service.AppSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 설정(SMTP/외부 API 키) 조회·저장.
 */
@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final AppSettingsService settingsService;

    public AdminSettingsController(AppSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<List<AppSetting>> list() {
        return ResponseEntity.ok(settingsService.findAll());
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> update(@RequestBody Map<String, String> body) {
        settingsService.setAll(body);
        return ResponseEntity.ok(Map.of("message", "설정이 저장되었습니다."));
    }
}
