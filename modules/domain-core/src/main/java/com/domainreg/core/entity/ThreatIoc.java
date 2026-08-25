package com.domainreg.core.entity;

import java.time.Instant;

/**
 * 외부 위협정보(KISA C-TAS 등)에서 수집해 캐싱한 악성 지표(IOC).
 */
public class ThreatIoc {

    public static final String TYPE_IP = "IP";
    public static final String TYPE_DOMAIN = "DOMAIN";

    private Long id;
    private String iocType;   // IP | DOMAIN
    private String iocValue;
    private String source;    // CTAS (향후 확장)
    private Instant firstSeen;
    private Instant lastSeen;
    private Instant createdAt;

    public static ThreatIoc create(String iocType, String iocValue, String source) {
        ThreatIoc t = new ThreatIoc();
        t.iocType = iocType;
        t.iocValue = iocValue;
        t.source = source;
        return t;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIocType() { return iocType; }
    public void setIocType(String iocType) { this.iocType = iocType; }

    public String getIocValue() { return iocValue; }
    public void setIocValue(String iocValue) { this.iocValue = iocValue; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getFirstSeen() { return firstSeen; }
    public void setFirstSeen(Instant firstSeen) { this.firstSeen = firstSeen; }

    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
