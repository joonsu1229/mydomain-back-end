package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.UserTermsAgreement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserTermsAgreementMapper {
    void insert(UserTermsAgreement agreement);
}
