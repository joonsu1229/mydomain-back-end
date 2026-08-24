package com.domainreg.core.vo;

public record AvailabilityResult(
    boolean available,
    Integer price,
    String message
) {
    public static AvailabilityResult available(int price) {
        return new AvailabilityResult(true, price, null);
    }

    public static AvailabilityResult unavailable(String message) {
        return new AvailabilityResult(false, null, message);
    }
}
