package com.domainreg.controller;

import com.domainreg.service.WhoisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicWhoisController {

    private final WhoisService whoisService;

    public PublicWhoisController(WhoisService whoisService) {
        this.whoisService = whoisService;
    }

    @GetMapping("/whois")
    public ResponseEntity<Map<String, Object>> lookup(@RequestParam("q") String query) {
        Map<String, Object> result = whoisService.lookup(query);
        return ResponseEntity.ok(result);
    }
}
