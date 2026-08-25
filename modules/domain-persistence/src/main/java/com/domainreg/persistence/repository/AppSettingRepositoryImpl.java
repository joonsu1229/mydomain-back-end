package com.domainreg.persistence.repository;

import com.domainreg.core.entity.AppSetting;
import com.domainreg.core.port.AppSettingRepository;
import com.domainreg.persistence.mapper.AppSettingMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AppSettingRepositoryImpl implements AppSettingRepository {

    private final AppSettingMapper mapper;

    public AppSettingRepositoryImpl(AppSettingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<AppSetting> findAll() {
        return mapper.findAll();
    }

    @Override
    public void upsertValue(String key, String value) {
        mapper.upsertValue(key, value);
    }
}
