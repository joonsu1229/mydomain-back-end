package com.domainreg.controller;

import com.domainreg.core.entity.DnsRecord;
import com.domainreg.security.UserPrincipal;
import com.domainreg.service.DnsRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.domainreg.service.ZoneFileImportService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/domains/{domainId}/dns")
public class DnsRecordController {

    private final DnsRecordService service;
    private final ZoneFileImportService zoneFileImportService;

    public DnsRecordController(DnsRecordService service, ZoneFileImportService zoneFileImportService) {
        this.service = service;
        this.zoneFileImportService = zoneFileImportService;
    }

    @GetMapping
    public ResponseEntity<List<DnsRecord>> list(@PathVariable Long domainId,
                                                  @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(service.getRecords(p.getUserId(), domainId));
    }

    @PostMapping
    public ResponseEntity<DnsRecord> create(@PathVariable Long domainId,
                                              @RequestBody DnsRecordRequest req,
                                              @AuthenticationPrincipal UserPrincipal p) {
        DnsRecord r = service.addRecord(p.getUserId(), domainId,
            req.type(), req.name(), req.content(), req.ttl(), req.priority());
        return ResponseEntity.status(HttpStatus.CREATED).body(r);
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<DnsRecord> update(@PathVariable Long domainId,
                                              @PathVariable Long recordId,
                                              @RequestBody DnsRecordRequest req,
                                              @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(service.updateRecord(p.getUserId(), domainId, recordId,
            req.type(), req.name(), req.content(), req.ttl(), req.priority()));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Map<String,String>> delete(@PathVariable Long domainId,
                                                       @PathVariable Long recordId,
                                                       @AuthenticationPrincipal UserPrincipal p) {
        service.deleteRecord(p.getUserId(), domainId, recordId);
        return ResponseEntity.ok(Map.of("message","DNS record deleted"));
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importZone(
            @PathVariable Long domainId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal p) {

        String zoneText = body.getOrDefault("zoneText", "");
        var results = zoneFileImportService.parse(zoneText);

        int imported = 0;
        int failed = 0;
        for (var r : results) {
            if (r.isSuccess()) {
                try {
                    service.addRecord(p.getUserId(), domainId,
                        r.type(), r.name(), r.content(), r.ttl(), r.priority());
                    imported++;
                } catch (Exception e) {
                    failed++;
                }
            } else {
                failed++;
            }
        }

        return ResponseEntity.ok(Map.of(
            "total", results.size(),
            "imported", imported,
            "failed", failed
        ));
    }

    public record DnsRecordRequest(String type, String name, String content, int ttl, Integer priority) {}
}
