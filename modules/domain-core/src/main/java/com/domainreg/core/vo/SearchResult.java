package com.domainreg.core.vo;

import java.util.List;

public record SearchResult(
    String query,
    String punycode,
    boolean available,
    Integer price,
    String currency,
    List<TldResult> tlds
) {
    public static SearchResult available(String query, String punycode, int price) {
        return new SearchResult(query, punycode, true, price, "KRW", List.of());
    }

    public static SearchResult taken(String query, String punycode) {
        return new SearchResult(query, punycode, false, null, "KRW", List.of());
    }

    public static SearchResult empty(String query) {
        return new SearchResult(query, query, false, null, "KRW", List.of());
    }

    public record TldResult(
        String tld,
        boolean available,
        Integer price,
        String registeredAt
    ) {}
}
