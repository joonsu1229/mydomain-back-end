package com.domainreg.core.service;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.enums.DomainStatus;

import java.util.Map;
import java.util.Set;

import static com.domainreg.core.enums.DomainStatus.*;

public class DomainStateMachine {

    private static final Map<DomainStatus, Set<DomainStatus>> transitions = Map.of(
        RESERVED,         Set.of(PENDING_PAYMENT, FAILED),
        PENDING_PAYMENT,  Set.of(ACTIVE, FAILED),
        ACTIVE,           Set.of(PRIVACY_ON, NS_UPDATING, EXPIRED),
        PRIVACY_ON,       Set.of(ACTIVE, EXPIRED),
        NS_UPDATING,      Set.of(ACTIVE, EXPIRED),
        FAILED,           Set.of(),
        EXPIRED,          Set.of()
    );

    public void transition(Domain domain, DomainStatus target) {
        Set<DomainStatus> allowed = transitions.getOrDefault(domain.getStatus(), Set.of());
        if (!allowed.contains(target)) {
            throw new IllegalStateException(
                "Invalid domain transition: " + domain.getStatus() + " -> " + target
                    + " (domain=" + domain.getNameUnicode() + ")");
        }
        domain.setStatus(target);
    }

    public boolean canTransition(DomainStatus current, DomainStatus target) {
        Set<DomainStatus> allowed = transitions.getOrDefault(current, Set.of());
        return allowed.contains(target);
    }
}
