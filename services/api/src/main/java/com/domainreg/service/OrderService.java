package com.domainreg.service;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.entity.Order;
import com.domainreg.core.entity.PlatformDomain;
import com.domainreg.core.enums.ProductType;
import com.domainreg.core.port.DomainRepository;
import com.domainreg.core.port.OrderRepository;
import com.domainreg.core.port.PlatformDomainRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.IDN;
import java.time.Duration;
import java.util.UUID;

@Service
public class OrderService {

    private final DomainRepository domainRepository;
    private final OrderRepository orderRepository;
    private final PlatformDomainRepository platformDomainRepository;
    private final StringRedisTemplate redis;

    public OrderService(DomainRepository domainRepository,
                        OrderRepository orderRepository,
                        PlatformDomainRepository platformDomainRepository,
                        StringRedisTemplate redis) {
        this.domainRepository = domainRepository;
        this.orderRepository = orderRepository;
        this.platformDomainRepository = platformDomainRepository;
        this.redis = redis;
    }

    /**
     * Create an order for a subdomain under a platform domain.
     */
    @Transactional
    public Order createOrder(Long userId, Long platformDomainId, String prefix) {
        String trimmedPrefix = prefix.trim().toLowerCase();

        // Validate prefix — alphanumeric + hyphens only
        if (!trimmedPrefix.matches("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$")) {
            throw new OrderException("INVALID_PREFIX",
                "서브도메인은 영문 소문자, 숫자, 하이픈만 사용할 수 있습니다.");
        }

        // Load platform domain
        PlatformDomain pd = platformDomainRepository.findById(platformDomainId)
            .orElseThrow(() -> new OrderException("PLATFORM_NOT_FOUND", "플랫폼 도메인을 찾을 수 없습니다."));
        if (!pd.isActive()) {
            throw new OrderException("PLATFORM_INACTIVE", "현재 사용할 수 없는 플랫폼 도메인입니다.");
        }

        // Build full domain name
        String fullUnicode = trimmedPrefix + "." + pd.getNameUnicode();
        String fullPunycode;
        try {
            fullPunycode = trimmedPrefix + "." + pd.getNamePunycode();
            IDN.toASCII(fullUnicode); // validate
        } catch (IllegalArgumentException e) {
            throw new OrderException("INVALID_DOMAIN", "유효하지 않은 도메인 이름입니다.");
        }

        // Distributed lock to prevent race condition on same domain name
        String lockKey = "lock:domain:" + fullPunycode;
        Boolean locked = redis.opsForValue()
            .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));

        if (!Boolean.TRUE.equals(locked)) {
            throw new OrderException("DOMAIN_TAKEN",
                "누군가 먼저 등록 중인 도메인입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            // Check uniqueness
            if (domainRepository.existsByPunycode(fullPunycode)) {
                throw new OrderException("DOMAIN_TAKEN", "이미 등록된 서브도메인입니다.");
            }

            // Extract TLD from platform domain name
            String tld = extractTld(pd.getNameUnicode());

            // Free subdomain: create domain as ACTIVE directly
            Domain domain = Domain.createFreeSubdomain(userId, platformDomainId, fullUnicode, fullPunycode, tld);
            domainRepository.save(domain);

            // Create zero-amount order for tracking
            String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Order order = Order.create(userId, domain.getId(), orderNumber, 0, ProductType.REGISTER);
            orderRepository.save(order);

            return order;
        } finally {
            // Release lock (ignore if already expired)
            redis.delete(lockKey);
        }
    }

    public Order getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다."));
        if (!order.getUserId().equals(userId)) {
            throw new OrderException("FORBIDDEN", "해당 주문에 접근할 수 없습니다.");
        }
        return order;
    }

    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new OrderException("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다."));
    }

    private String extractTld(String domainName) {
        String lower = domainName.toLowerCase();
        if (lower.endsWith(".한국")) return ".한국";
        if (lower.endsWith(".kr")) return ".kr";
        if (lower.endsWith(".com")) return ".com";
        if (lower.endsWith(".net")) return ".net";
        if (lower.endsWith(".org")) return ".org";
        int lastDot = lower.lastIndexOf('.');
        if (lastDot >= 0) return lower.substring(lastDot);
        return ".kr";
    }

    public static class OrderException extends RuntimeException {
        private final String code;
        public OrderException(String code, String message) { super(message); this.code = code; }
        public String getCode() { return code; }
    }
}
