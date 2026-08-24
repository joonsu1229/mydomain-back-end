package com.domainreg.core.entity;

import java.time.Instant;

public class DnsRecord {
    private Long id;
    private Long domainId;
    private String recordType;  // A, AAAA, CNAME, MX, TXT, NS, SRV
    private String name;        // subdomain prefix, @ for root
    private String content;     // value
    private int ttl;
    private Integer priority;   // for MX/SRV
    private Instant createdAt;
    private Instant updatedAt;

    public static DnsRecord create(Long domainId, String recordType, String name,
                                    String content, int ttl, Integer priority) {
        DnsRecord r = new DnsRecord();
        r.domainId = domainId;
        r.recordType = recordType.toUpperCase();
        r.name = name;
        r.content = content;
        r.ttl = ttl > 0 ? ttl : 3600;
        r.priority = priority;
        return r;
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDomainId() { return domainId; }
    public void setDomainId(Long v) { this.domainId = v; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String v) { this.recordType = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public int getTtl() { return ttl; }
    public void setTtl(int v) { this.ttl = v; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer v) { this.priority = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
