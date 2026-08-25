package com.domainreg.core.entity;

import java.time.Instant;

/**
 * 관리자 페이지에서 설정하는 앱 설정(SMTP, 외부 API 키 등) key-value 저장.
 */
public class AppSetting {

    private String key;        // PK
    private String value;
    private String description;
    private Instant updatedAt;

    public static AppSetting of(String key, String value, String description) {
        AppSetting s = new AppSetting();
        s.key = key;
        s.value = value;
        s.description = description;
        return s;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
