package com.domainreg.controller;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.enums.DomainStatus;
import com.domainreg.core.port.DomainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final DomainRepository domainRepository;

    @Value("${app.webhook.secret:webhook-secret-change-me}")
    private String webhookSecret;

    public WebhookController(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    @PostMapping("/registrar")
    public ResponseEntity<Map<String, String>> registrarCallback(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody RegistrarEvent event) {

        if (!webhookSecret.equals(secret)) {
            log.warn("Webhook received with invalid secret");
            return ResponseEntity.status(401).body(Map.of("message", "Invalid secret"));
        }

        log.info("Registrar webhook: type={}, ref={}", event.type(), event.registrarRef());

        switch (event.type()) {
            case "REGISTRATION_COMPLETE" -> handleRegistrationComplete(event);
            case "REGISTRATION_FAILED" -> handleRegistrationFailed(event);
            case "NS_UPDATE_COMPLETE" -> handleNsUpdateComplete(event);
            case "DNS_SYNC_COMPLETE" -> handleDnsSyncComplete(event);
            default -> log.info("Unknown webhook event type: {}", event.type());
        }

        return ResponseEntity.ok(Map.of("message", "Processed"));
    }

    private void handleRegistrationComplete(RegistrarEvent event) {
        domainRepository.findByPunycode(event.domainPunycode()).ifPresent(domain -> {
            domain.markRegistered(event.registrarRef(),
                java.time.LocalDate.ofInstant(event.expiresAt(), java.time.ZoneOffset.UTC));
            domainRepository.save(domain);
            log.info("Domain registered via webhook: {}", domain.getNameUnicode());
        });
    }

    private void handleRegistrationFailed(RegistrarEvent event) {
        domainRepository.findByPunycode(event.domainPunycode()).ifPresent(domain -> {
            domainRepository.updateStatus(domain.getId(), DomainStatus.FAILED);
            log.error("Domain registration failed via webhook: {}", domain.getNameUnicode());
        });
    }

    private void handleNsUpdateComplete(RegistrarEvent event) {
        domainRepository.findByPunycode(event.domainPunycode()).ifPresent(domain -> {
            domainRepository.updateStatus(domain.getId(), DomainStatus.ACTIVE);
            log.info("NS update confirmed via webhook: {}", domain.getNameUnicode());
        });
    }

    private void handleDnsSyncComplete(RegistrarEvent event) {
        log.info("DNS sync confirmed via webhook: {}", event.domainPunycode());
    }

    public record RegistrarEvent(
        String type,
        String registrarRef,
        String domainPunycode,
        java.time.Instant expiresAt,
        String message
    ) {}
}
