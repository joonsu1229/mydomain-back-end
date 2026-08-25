package com.domainreg.threatintel;

import com.domainreg.core.entity.ThreatIoc;
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * KISA C-TAS 위협정보 조회 클라이언트 (best-effort).
 * API 키/주소는 관리자 설정(DB)에서 읽는다. 정확한 응답 포맷은 계약에 따라 다르므로,
 * JSON 응답에서 IPv4/도메인 문자열을 방어적으로 추출한다. 계약 확정 후 파서를 정밀화할 것.
 * 호출 실패/타임아웃/키 미설정은 빈 목록(fail-open)을 반환한다.
 */
@Component
public class CtasClient {

    private static final Logger log = LoggerFactory.getLogger(CtasClient.class);
    private static final Pattern IPV4 = Pattern.compile(
        "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");

    private static final String[] LIST_FIELDS = {"iocs", "data", "indicators", "list", "items"};
    private static final String[] VALUE_FIELDS = {"value", "ioc", "iocValue", "ip", "domain", "url"};

    private final AppSettingsService settings;
    private final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final RestClient rest;

    public CtasClient(AppSettingsService settings) {
        this.settings = settings;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.rest = RestClient.builder()
            .requestFactory(factory)
            .messageConverters(List.of(new MappingJackson2HttpMessageConverter(mapper)))
            .build();
    }

    public List<ThreatIoc> fetchIocs() {
        String apiKey = settings.getOrDefault("ctas.api-key", "");
        String baseUrl = settings.getOrDefault("ctas.base-url", "");
        if (apiKey.isBlank() || baseUrl.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = rest.get()
                .uri(baseUrl)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .body(JsonNode.class);
            if (root == null) {
                return List.of();
            }
            List<ThreatIoc> iocs = new ArrayList<>();
            collectIocs(root, iocs);
            return iocs;
        } catch (Exception e) {
            log.warn("C-TAS fetch failed: {}", e.getMessage());
            return List.of();
        }
    }

    private void collectIocs(JsonNode node, List<ThreatIoc> out) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectIocs(child, out);
            }
            return;
        }
        if (node.isObject()) {
            for (String key : LIST_FIELDS) {
                JsonNode child = node.get(key);
                if (child != null && (child.isArray() || child.isObject())) {
                    collectIocs(child, out);
                }
            }
            for (String key : VALUE_FIELDS) {
                addScalar(node.get(key), out);
            }
            return;
        }
        addScalar(node, out);
    }

    private void addScalar(JsonNode v, List<ThreatIoc> out) {
        if (v == null || !v.isTextual()) {
            return;
        }
        String s = v.asText().trim();
        if (s.isEmpty()) {
            return;
        }
        String type = IPV4.matcher(s).matches() ? ThreatIoc.TYPE_IP : ThreatIoc.TYPE_DOMAIN;
        out.add(ThreatIoc.create(type, s, "CTAS"));
    }
}
