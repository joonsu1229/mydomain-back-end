package com.domainreg.core.port;

import com.domainreg.core.entity.DnsRecord;
import com.domainreg.core.vo.*;

import java.util.List;

public interface RegistrarClient {
    AvailabilityResult checkAvailability(String punycodeName);
    RegisterResult register(RegisterCommand cmd);
    void updateNameservers(String zoneName, String domainName, List<Nameserver> ns);
    void syncDnsRecords(String zoneName, String domainName, List<DnsRecord> records);
    void setPrivacy(String registrarRef, boolean enabled);
    DomainInfo getDomain(String registrarRef);
}
