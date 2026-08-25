package com.domainreg.threatintel;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 사전 차단/검증 설정, {@code app.security.*} 바인딩.
 */
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private boolean keywordFilterEnabled = true;
    private boolean dnsblEnabled = true;
    private List<String> dnsblZones = new ArrayList<>(List.of("zen.spamhaus.org"));

    public boolean isKeywordFilterEnabled() { return keywordFilterEnabled; }
    public void setKeywordFilterEnabled(boolean keywordFilterEnabled) { this.keywordFilterEnabled = keywordFilterEnabled; }

    public boolean isDnsblEnabled() { return dnsblEnabled; }
    public void setDnsblEnabled(boolean dnsblEnabled) { this.dnsblEnabled = dnsblEnabled; }

    public List<String> getDnsblZones() { return dnsblZones; }
    public void setDnsblZones(List<String> dnsblZones) { this.dnsblZones = dnsblZones; }
}
