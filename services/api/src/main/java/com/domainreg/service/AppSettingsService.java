package com.domainreg.service;

import com.domainreg.core.entity.AppSetting;
import com.domainreg.core.port.AppSettingRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 설정(SMTP/API 키) key-value 조회/저장.
 * 값은 60초 캐시하고, 저장 시 즉시 무효화한다.
 */
@Service
public class AppSettingsService {

    private static final long CACHE_TTL_MS = 60_000;

    private final AppSettingRepository repository;
    private volatile Map<String, String> cache;
    private volatile long cacheLoadedAt = 0;

    public AppSettingsService(AppSettingRepository repository) {
        this.repository = repository;
    }

    public List<AppSetting> findAll() {
        return repository.findAll();
    }

    public String get(String key) {
        return snapshot().get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        String v = snapshot().get(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }

    public void setAll(Map<String, String> updates) {
        if (updates == null) {
            return;
        }
        for (Map.Entry<String, String> e : updates.entrySet()) {
            repository.upsertValue(e.getKey(), e.getValue() == null ? "" : e.getValue());
        }
        cache = null;
    }

    private Map<String, String> snapshot() {
        Map<String, String> c = cache;
        long now = System.currentTimeMillis();
        if (c == null || now - cacheLoadedAt > CACHE_TTL_MS) {
            synchronized (this) {
                if (cache == null || now - cacheLoadedAt > CACHE_TTL_MS) {
                    Map<String, String> m = new HashMap<>();
                    for (AppSetting s : repository.findAll()) {
                        m.put(s.getKey(), s.getValue());
                    }
                    cache = m;
                    cacheLoadedAt = System.currentTimeMillis();
                }
                c = cache;
            }
        }
        return c;
    }
}
