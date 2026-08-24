package com.domainreg.registrar.powerdns;

/**
 * Thrown when the PowerDNS HTTP API returns an error or the zone cannot be resolved.
 */
public class PowerDnsApiException extends RuntimeException {
    public PowerDnsApiException(String message) {
        super(message);
    }

    public PowerDnsApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
