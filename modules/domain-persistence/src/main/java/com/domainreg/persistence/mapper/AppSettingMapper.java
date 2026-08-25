package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.AppSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppSettingMapper {
    List<AppSetting> findAll();
    void upsertValue(@Param("key") String key, @Param("value") String value);
}
