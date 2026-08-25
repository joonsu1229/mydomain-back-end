package com.domainreg.core.entity;

import java.time.Instant;

/**
 * 도메인/레코드 이름 사전 차단용 블랙리스트 키워드.
 */
public class BlocklistKeyword {

    public static final String CATEGORY_IMPERSONATION = "IMPERSONATION"; // 금융/포털 사칭
    public static final String CATEGORY_RESERVED = "RESERVED";           // 시스템 예약어

    private Long id;
    private String keyword;
    private String category;
    private boolean enabled;
    private String note;
    private Instant createdAt;
    private Instant updatedAt;

    public static BlocklistKeyword create(String keyword, String category, String note) {
        BlocklistKeyword k = new BlocklistKeyword();
        k.keyword = keyword;
        k.category = category;
        k.note = note;
        k.enabled = true;
        return k;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
