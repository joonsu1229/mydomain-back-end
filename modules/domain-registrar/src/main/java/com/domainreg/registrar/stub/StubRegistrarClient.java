package com.domainreg.registrar.stub;

import com.domainreg.core.entity.DnsRecord;
import com.domainreg.core.port.RegistrarClient;
import com.domainreg.core.vo.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class StubRegistrarClient implements RegistrarClient {

    private static final int DEFAULT_PRICE = 19_800; // KRW
    private final AtomicLong counter = new AtomicLong(1000);

    @Override
    public AvailabilityResult checkAvailability(String punycodeName) {
        // Simulate slight latency
        simulateLatency();

        // Reserved names for testing
        if (punycodeName.startsWith("xn--taken") || punycodeName.contains("registered")) {
            return AvailabilityResult.unavailable("이미 등록된 도메인입니다.");
        }
        if (punycodeName.startsWith("xn--premium")) {
            return AvailabilityResult.available(99_000);
        }
        return AvailabilityResult.available(DEFAULT_PRICE);
    }

    @Override
    public RegisterResult register(RegisterCommand cmd) {
        simulateLatency();
        String ref = "STUB-" + counter.incrementAndGet();
        return RegisterResult.success(ref, LocalDate.now().plusYears(1));
    }

    @Override
    public void updateNameservers(String zoneName, String domainName, List<Nameserver> ns) {
        simulateLatency();
        // no-op in stub — nameservers are tracked in our DB
    }

    @Override
    public void syncDnsRecords(String zoneName, String domainName, List<DnsRecord> records) {
        simulateLatency();
        // no-op in stub — DNS records are tracked in our DB
    }

    @Override
    public void setPrivacy(String registrarRef, boolean enabled) {
        simulateLatency();
        // no-op in stub — privacy is tracked in our DB
    }

    @Override
    public DomainInfo getDomain(String registrarRef) {
        simulateLatency();
        return DomainInfo.builder()
            .registrarRef(registrarRef)
            .expiresAt(LocalDate.now().plusYears(1))
            .nameservers(List.of())
            .privacyEnabled(false)
            .build();
    }

    private void simulateLatency() {
        try {
            Thread.sleep(100 + (long) (Math.random() * 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
