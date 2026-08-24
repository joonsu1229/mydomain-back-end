package com.domainreg.controller;

import com.domainreg.core.entity.DnsTemplate;
import com.domainreg.security.UserPrincipal;
import com.domainreg.service.DnsTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
public class DnsTemplateController {

    private final DnsTemplateService service;

    public DnsTemplateController(DnsTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<DnsTemplate>> list(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(service.getTemplates(p.getUserId()));
    }

    @PostMapping
    public ResponseEntity<DnsTemplate> create(@RequestBody TemplateRequest req,
                                               @AuthenticationPrincipal UserPrincipal p) {
        DnsTemplate t = service.createTemplate(p.getUserId(), req.name(),
            req.description(), req.recordsJson());
        return ResponseEntity.status(HttpStatus.CREATED).body(t);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DnsTemplate> update(@PathVariable Long id,
                                               @RequestBody TemplateRequest req,
                                               @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(service.updateTemplate(p.getUserId(), id,
            req.name(), req.description(), req.recordsJson()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String,String>> delete(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserPrincipal p) {
        service.deleteTemplate(p.getUserId(), id);
        return ResponseEntity.ok(Map.of("message", "Template deleted"));
    }

    public record TemplateRequest(String name, String description, String recordsJson) {}
}
