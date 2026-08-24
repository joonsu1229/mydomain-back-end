package com.domainreg.core.entity;

import java.time.Instant;

public class DnsTemplate {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String recordsJson;
    private Instant createdAt;
    private Instant updatedAt;

    public static DnsTemplate create(Long userId, String name, String description, String recordsJson) {
        DnsTemplate t = new DnsTemplate();
        t.userId = userId;
        t.name = name;
        t.description = description;
        t.recordsJson = recordsJson;
        return t;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getRecordsJson() { return recordsJson; }
    public void setRecordsJson(String v) { this.recordsJson = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
