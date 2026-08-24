package com.domainreg.registrar.cloudflare;

/**
 * Raised when a Cloudflare API call fails or is misconfigured.
 */
public class CloudflareApiException extends RuntimeException {

    public CloudflareApiException(String message) {
        super(message);
    }

    public CloudflareApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
