package com.domainreg.core.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class Terms {

    public static final String TYPE_TERMS = "TERMS";
    public static final String TYPE_PRIVACY = "PRIVACY";

    private Long id;
    private String type;
    private int version;
    private String title;
    private String content;
    private boolean isCurrent;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Aggregate (admin list only) — number of users who agreed to this version.
    private long agreementCount;

    public static Terms create(String type, String title, String content) {
        Terms t = new Terms();
        t.type = type;
        t.title = title;
        t.content = content;
        return t;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @JsonProperty("isCurrent")
    public boolean isCurrent() { return isCurrent; }
    @JsonProperty("isCurrent")
    public void setCurrent(boolean current) { this.isCurrent = current; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public long getAgreementCount() { return agreementCount; }
    public void setAgreementCount(long agreementCount) { this.agreementCount = agreementCount; }
}
