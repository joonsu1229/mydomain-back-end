package com.domainreg.core.port;

import com.domainreg.core.entity.AppSetting;

import java.util.List;

/**
 * 관리자 설정(SMTP/API 키) key-value 저장소.
 */
public interface AppSettingRepository {
    List<AppSetting> findAll();
    void upsertValue(String key, String value);
}
