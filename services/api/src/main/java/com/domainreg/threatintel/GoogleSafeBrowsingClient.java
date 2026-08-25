package com.domainreg.threatintel;

import com.domainreg.service.AppSettingsService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Google Safe Browsing v4 {@code threatMatches:find} 클라이언트.
 * API 키는 관리자 설정(DB)에서 읽는다. URL이 악성으로 분류되면 true.
 * 호출 실패/타임아웃/키 미설정은 fail-open(false).
 */
@Component
public class GoogleSafeBrowsingClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleSafeBrowsingClient.class);
    private static final String CLIENT_ID = "mydomain-reg";
    private static final String CLIENT_VERSION = "1.0.0";
    private static final String BASE_URL = "https://safebrowsing.googleapis.com/v4";

    private final AppSettingsService settings;
    private final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final RestClient rest;

    public GoogleSafeBrowsingClient(AppSettingsService settings) {
        this.settings = settings;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(2000);
        this.rest = RestClient.builder()
            .requestFactory(factory)
            .messageConverters(List.of(new MappingJackson2HttpMessageConverter(mapper)))
            .build();
    }

    public boolean isThreatUrl(String url) {
        String apiKey = settings.getOrDefault("safebrowsing.api-key", "");
        if (apiKey.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> body = Map.of(
                "client", Map.of("clientId", CLIENT_ID, "clientVersion", CLIENT_VERSION),
                "threatInfo", Map.of(
                    "threatTypes", List.of(
                        "MALWARE", "SOCIAL_ENGINEERING",
                        "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"),
                    "platformTypes", List.of("ANY_PLATFORM"),
                    "threatEntryTypes", List.of("URL"),
                    "threatEntries", List.of(Map.of("url", url))
                )
            );

            String target = BASE_URL + "/threatMatches:find?key="
                + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            JsonNode resp = rest.post()
                .uri(target)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

            if (resp == null) {
                return false;
            }
            JsonNode matches = resp.get("matches");
            return matches != null && matches.isArray() && matches.size() > 0;
        } catch (Exception e) {
            log.warn("Safe Browsing check failed for {}: {}", url, e.getMessage());
            return false;
        }
    }
}
