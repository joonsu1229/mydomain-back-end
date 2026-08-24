package com.domainreg.core.vo;

import java.time.LocalDate;
import java.util.List;

public record DomainInfo(
    String registrarRef,
    LocalDate expiresAt,
    List<Nameserver> nameservers,
    boolean privacyEnabled
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String registrarRef;
        private LocalDate expiresAt;
        private List<Nameserver> nameservers = List.of();
        private boolean privacyEnabled;

        public Builder registrarRef(String ref) { this.registrarRef = ref; return this; }
        public Builder expiresAt(LocalDate d) { this.expiresAt = d; return this; }
        public Builder nameservers(List<Nameserver> ns) { this.nameservers = ns; return this; }
        public Builder privacyEnabled(boolean p) { this.privacyEnabled = p; return this; }
        public DomainInfo build() { return new DomainInfo(registrarRef, expiresAt, nameservers, privacyEnabled); }
    }
}
