package com.domainreg.controller;

import com.domainreg.core.entity.BlocklistKeyword;
import com.domainreg.service.BlocklistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 사전 차단 키워드(예약어/금지어) 관리자 CRUD.
 */
@RestController
@RequestMapping("/api/admin/blocklist")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlocklistController {

    private final BlocklistService blocklistService;

    public AdminBlocklistController(BlocklistService blocklistService) {
        this.blocklistService = blocklistService;
    }

    @GetMapping
    public ResponseEntity<List<BlocklistKeyword>> list() {
        return ResponseEntity.ok(blocklistService.findAll());
    }

    @PostMapping
    public ResponseEntity<BlocklistKeyword> create(@RequestBody Map<String, String> body) {
        BlocklistKeyword k = blocklistService.add(
            body.get("keyword"), body.get("category"), body.get("note"));
        return ResponseEntity.status(HttpStatus.CREATED).body(k);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlocklistKeyword> update(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> body) {
        Boolean enabled = body.get("enabled") == null ? null : (Boolean) body.get("enabled");
        BlocklistKeyword k = blocklistService.update(id,
            (String) body.get("keyword"),
            (String) body.get("category"),
            (String) body.get("note"),
            enabled);
        return ResponseEntity.ok(k);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        blocklistService.delete(id);
        return ResponseEntity.ok(Map.of("message", "키워드가 삭제되었습니다."));
    }
}
