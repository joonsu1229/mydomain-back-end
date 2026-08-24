package com.domainreg.controller;

import com.domainreg.core.entity.Terms;
import com.domainreg.service.TermsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TermsController {

    private final TermsService termsService;

    public TermsController(TermsService termsService) {
        this.termsService = termsService;
    }

    /** Public — current (published) terms & privacy policy. */
    @GetMapping("/terms/current")
    public ResponseEntity<List<Terms>> getCurrent() {
        return ResponseEntity.ok(termsService.getCurrentTerms());
    }

    /** Admin — list all versions. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/terms")
    public ResponseEntity<List<Terms>> listAll() {
        return ResponseEntity.ok(termsService.listAll());
    }

    /** Admin — create a new draft version. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/terms")
    public ResponseEntity<Terms> create(@RequestBody Map<String, String> body) {
        Terms t = termsService.createDraft(body.get("type"), body.get("title"), body.get("content"));
        return ResponseEntity.status(HttpStatus.CREATED).body(t);
    }

    /** Admin — edit a draft's title/content. */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/terms/{id}")
    public ResponseEntity<Terms> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(termsService.updateDraft(id, body.get("title"), body.get("content")));
    }

    /** Admin — publish a draft as the current version. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/terms/{id}/publish")
    public ResponseEntity<Terms> publish(@PathVariable Long id) {
        return ResponseEntity.ok(termsService.publish(id));
    }

    /** Admin — delete a draft. */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/terms/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        termsService.deleteDraft(id);
        return ResponseEntity.ok(Map.of("message", "초안이 삭제되었습니다."));
    }
}
