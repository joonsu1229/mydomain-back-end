package com.domainreg.persistence.repository;

import com.domainreg.core.entity.UserTermsAgreement;
import com.domainreg.core.port.UserTermsAgreementRepository;
import com.domainreg.persistence.mapper.UserTermsAgreementMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserTermsAgreementRepositoryImpl implements UserTermsAgreementRepository {

    private final UserTermsAgreementMapper mapper;

    public UserTermsAgreementRepositoryImpl(UserTermsAgreementMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(UserTermsAgreement agreement) {
        mapper.insert(agreement);
    }
}
