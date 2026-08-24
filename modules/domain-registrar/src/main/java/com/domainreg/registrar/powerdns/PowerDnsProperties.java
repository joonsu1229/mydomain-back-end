package com.domainreg.registrar.powerdns;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PowerDNS authoritative API configuration, bound from {@code app.powerdns.*}.
 *
 * <ul>
 *   <li>{@code base-url} — PowerDNS webserver/API base URL (defaults to the local API).</li>
 *   <li>{@code api-key} — value of {@code api-key} in {@code pdns.conf}.</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.powerdns")
public class PowerDnsProperties {

    private String baseUrl = "http://127.0.0.1:8081";
    private String apiKey;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
