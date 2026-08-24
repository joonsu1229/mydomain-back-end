package com.domainreg.config;

import com.domainreg.core.service.DomainStateMachine;
import com.domainreg.core.service.PaidGateEnforcer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public DomainStateMachine domainStateMachine() {
        return new DomainStateMachine();
    }

    @Bean
    public PaidGateEnforcer paidGateEnforcer() {
        return new PaidGateEnforcer();
    }
}
