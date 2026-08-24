package com.domainreg.registrar.stub;

import com.domainreg.core.port.RegistrarClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StubRegistrarConfig {

    @Bean
    @ConditionalOnProperty(name = "app.registrar.mode", havingValue = "stub", matchIfMissing = true)
    public RegistrarClient stubRegistrarClient() {
        return new StubRegistrarClient();
    }
}
