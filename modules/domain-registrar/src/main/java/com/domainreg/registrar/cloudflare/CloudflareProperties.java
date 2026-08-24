package com.domainreg.registrar.cloudflare;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cloudflare API configuration, bound from {@code app.cloudflare.*}.
 *
 * <ul>
 *   <li>{@code api-token} — a scoped API token with at least {@code Zone:Read} and
 *       {@code DNS:Edit} permissions on the target zone.</li>
 *   <li>{@code base-url} — Cloudflare API base URL (defaults to the v4 endpoint).</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.cloudflare")
public class CloudflareProperties {

    private String apiToken;
    private String baseUrl = "https://api.cloudflare.com/client/v4";

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

}
