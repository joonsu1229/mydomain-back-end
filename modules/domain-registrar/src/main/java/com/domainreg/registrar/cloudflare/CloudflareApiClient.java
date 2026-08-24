package com.domainreg.registrar.cloudflare;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin HTTP client over the Cloudflare v4 API. Only the DNS-record subset that the
 * platform needs is implemented (zone lookup + record CRUD). Zone IDs are cached in
 * memory since they are stable.
 */
public class CloudflareApiClient {

    private static final String DEFAULT_BASE = "https://api.cloudflare.com/client/v4";
    private static final int LIST_PAGE_SIZE = 100;

    private final RestClient rest;
    private final Map<String, String> zoneIdCache = new ConcurrentHashMap<>();

    public CloudflareApiClient(String apiToken, String baseUrl) {
        ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.rest = RestClient.builder()
            .baseUrl((baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE : baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiToken)
            .messageConverters(List.of(new MappingJackson2HttpMessageConverter(mapper)))
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                byte[] body = response.getBody().readAllBytes();
                throw new CloudflareApiException(
                    "Cloudflare API error " + response.getStatusCode() + ": " + new String(body));
            })
            .build();
    }

    /** Resolve a zone id by zone name (e.g. {@code domon.kr}), caching the result. */
    public String resolveZoneId(String zoneName) {
        return zoneIdCache.computeIfAbsent(zoneName, this::fetchZoneId);
    }

    private String fetchZoneId(String zoneName) {
        CloudflareResponse<List<Zone>> resp = rest.get()
            .uri(ub -> ub.path("/zones")
                .queryParam("name", zoneName)
                .queryParam("status", "active")
                .build())
            .retrieve()
            .body(new ParameterizedTypeReference<CloudflareResponse<List<Zone>>>() {});
        if (resp == null || !resp.success() || resp.result() == null || resp.result().isEmpty()) {
            throw new CloudflareApiException("Cloudflare zone not found or not active: " + zoneName);
        }
        return resp.result().get(0).id();
    }

    /** List the zone's DNS records (up to the first page of {@value #LIST_PAGE_SIZE}). */
    public List<DnsRecordDto> listDnsRecords(String zoneId) {
        CloudflareResponse<List<DnsRecordDto>> resp = rest.get()
            .uri(ub -> ub.path("/zones/{zoneId}/dns_records")
                .queryParam("per_page", LIST_PAGE_SIZE)
                .build(zoneId))
            .retrieve()
            .body(new ParameterizedTypeReference<CloudflareResponse<List<DnsRecordDto>>>() {});
        return (resp != null && resp.success() && resp.result() != null) ? resp.result() : List.of();
    }

    public DnsRecordDto createDnsRecord(String zoneId, DnsRecordRequest record) {
        CloudflareResponse<DnsRecordDto> resp = rest.post()
            .uri(ub -> ub.path("/zones/{zoneId}/dns_records").build(zoneId))
            .body(record)
            .retrieve()
            .body(new ParameterizedTypeReference<CloudflareResponse<DnsRecordDto>>() {});
        if (resp == null || !resp.success() || resp.result() == null) {
            throw new CloudflareApiException("Cloudflare failed to create DNS record " + record.name());
        }
        return resp.result();
    }

    public DnsRecordDto updateDnsRecord(String zoneId, String recordId, DnsRecordRequest record) {
        CloudflareResponse<DnsRecordDto> resp = rest.patch()
            .uri(ub -> ub.path("/zones/{zoneId}/dns_records/{recordId}").build(zoneId, recordId))
            .body(record)
            .retrieve()
            .body(new ParameterizedTypeReference<CloudflareResponse<DnsRecordDto>>() {});
        if (resp == null || !resp.success() || resp.result() == null) {
            throw new CloudflareApiException("Cloudflare failed to update DNS record " + record.name());
        }
        return resp.result();
    }

    public void deleteDnsRecord(String zoneId, String recordId) {
        CloudflareResponse<DnsRecordDto> resp = rest.delete()
            .uri(ub -> ub.path("/zones/{zoneId}/dns_records/{recordId}").build(zoneId, recordId))
            .retrieve()
            .body(new ParameterizedTypeReference<CloudflareResponse<DnsRecordDto>>() {});
        if (resp == null || !resp.success()) {
            throw new CloudflareApiException("Cloudflare failed to delete DNS record " + recordId);
        }
    }

    // -- DTOs --

    public record Zone(String id, String name, String status) {}

    /** Request body for creating/updating a record. */
    public record DnsRecordRequest(
        String type,
        String name,
        String content,
        Integer ttl,
        Integer priority,
        Boolean proxied,
        String comment
    ) {}

    /** Record returned by Cloudflare (includes the zone-scoped {@code id}). */
    public record DnsRecordDto(
        String id,
        String type,
        String name,
        String content,
        Integer ttl,
        Integer priority,
        Boolean proxied,
        String comment
    ) {}

    /** Standard Cloudflare envelope: {@code { success, errors, result }}. */
    public record CloudflareResponse<T>(boolean success, List<CloudflareError> errors, T result) {}

    public record CloudflareError(int code, String message) {}
}
