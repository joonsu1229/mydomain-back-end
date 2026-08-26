package com.domainreg.registrar.powerdns;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Thin HTTP client over the PowerDNS authoritative REST API ({@code /api/v1}).
 * Only the zone + rrsets subset that this platform needs is implemented.
 */
public class PowerDnsApiClient {

    private final RestClient rest;

    public PowerDnsApiClient(String baseUrl, String apiKey) {
        ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.rest = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("X-API-Key", apiKey)
            .messageConverters(List.of(new MappingJackson2HttpMessageConverter(mapper)))
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                byte[] body = response.getBody().readAllBytes();
                throw new PowerDnsApiException(
                    "PowerDNS API error " + response.getStatusCode() + ": " + new String(body));
            })
            .build();
    }

    /** Fetch a zone including its rrsets. */
    public Zone getZone(String zoneName) {
        Zone zone = rest.get()
            .uri("/api/v1/servers/localhost/zones/{zone}", canonical(zoneName))
            .retrieve()
            .body(Zone.class);
        if (zone == null) {
            throw new PowerDnsApiException("PowerDNS zone not found: " + zoneName);
        }
        return zone;
    }

    /** Apply a batch of rrset changes (DELETE / REPLACE) to a zone. */
    public void patchRrsets(String zoneName, List<RrsetChange> rrsets) {
        if (rrsets == null || rrsets.isEmpty()) {
            return;
        }
        rest.patch()
            .uri("/api/v1/servers/localhost/zones/{zone}", canonical(zoneName))
            .body(Map.of("rrsets", rrsets))
            .retrieve()
            .body(new ParameterizedTypeReference<Void>() {});
    }

    /** Queue a NOTIFY so secondaries (HE.net) immediately re-fetch the changed zone. */
    public void notify(String zoneName) {
        rest.put()
            .uri("/api/v1/servers/localhost/zones/{zone}/notify", canonical(zoneName))
            .retrieve()
            .body(new ParameterizedTypeReference<Void>() {});
    }

    private static String canonical(String zoneName) {
        return zoneName.endsWith(".") ? zoneName : zoneName + ".";
    }

    // -- DTOs --

    public record Zone(String id, String name, List<Rrset> rrsets) {}

    public record Rrset(String name, String type, Integer ttl, List<Record> records) {}

    public record Record(String content, Boolean disabled) {}

    /** A single rrset mutation for the PATCH body. */
    public record RrsetChange(String name, String type, Integer ttl, String changetype, List<Record> records) {}
}
