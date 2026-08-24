# 🔐 인증 & 세션 (JWT + Redis)

> 기술 면접 대비용 — 로그인/인증 흐름, JWT 설계, Redis를 활용한 세션 관리 전략을 정리한 문서입니다.

---

## 1. 전체 인증 흐름 (30초 요약)

```
회원가입 → 이메일 인증 → 로그인(access+refresh 발급) → API 호출(access) 
        → 만료 시 refresh로 재발급 → 로그아웃 시 refresh 삭제
```

- **Access Token**: 15분, 메모리에만 보관 (매 요청마다 헤더로 전송)
- **Refresh Token**: 7일, Redis에 저장 (`refresh:{userId}`)
- 서버는 **무상태(stateless)** — 세션을 서버 메모리에 안 두고 Redis에 둠

---

## 2. JWT란?

**JWT(JSON Web Token)** 는 사용자 정보를 담은 **서명된 토큰**이다.

```
Header.Payload.Signature
```

- **Header**: 알고리즘 정보 (`HS256`)
- **Payload(Claims)**: `userId`, `loginId`, `email`, `role`, 만료시간
- **Signature**: 비밀키로 서명 → 위변조 방지

**핵심 특징**: 서버가 토큰을 **저장하지 않고도** 검증할 수 있다 (서명만 확인하면 됨).

### 이 프로젝트의 JWT 설계

```java
// Access Token (15분)
Jwts.builder()
    .subject(userId)            // userId
    .claim("loginId", loginId)
    .claim("email", email)
    .claim("role", role)        // USER / ADMIN
    .expiration(15분 후)
    .signWith(key)              // HMAC-SHA256
```

- Access Token 만료: **15분** (짧게 → 탈취 피해 최소화)
- Refresh Token 만료: **7일** (길게 → 재로그인 빈도 낮춤)

---

## 3. 왜 Access / Refresh를 분리했나?

| | Access Token | Refresh Token |
|---|---|---|
| 수명 | 15분 (짧음) | 7일 (김) |
| 보관 위치 | 클라이언트 메모리 | Redis (서버) |
| 용도 | API 인증 | 재발급용 |
| 노출 시 피해 | 15분만 위험 | 즉시 차단 가능 |

**이유**: Access Token은 매 요청마다 노출되므로 **짧게** 둬서 탈취 피해를 줄이고, Refresh Token은 **서버(Redis)에서 제어**해 즉시 폐기할 수 있게 한다.

---

## 4. 로그인 플로우 (상세)

`POST /api/auth/login` → `AuthService.login()`

```java
public AuthToken login(String loginId, String password, String ip) {
    User user = userRepository.findByLoginId(loginId)
        .orElseThrow(...);  // 아이디/비번 불일치 → 401

    // 1. 계정 잠금 확인 (로그인 5회 실패 시 15분 잠금)
    if (user.isLocked()) throw ACCOUNT_LOCKED;

    // 2. 비밀번호 검증 (BCrypt)
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
        recordLoginFailure();   // 실패 횟수 +1
        if (5회 초과) lockAccount(15분);
        throw INVALID_CREDENTIALS;
    }

    // 3. 이메일 인증 여부 확인
    if (!user.isEmailVerified()) throw EMAIL_NOT_VERIFIED;

    // 4. 성공 → 토큰 발급
    recordLoginSuccess();       // 실패 카운트 리셋, last_login 갱신
    return issueTokens(user);
}
```

### 토큰 발급 (`issueTokens`)

```java
String accessToken  = createAccessToken(user);   // 15분
String refreshToken = createRefreshToken(user);  // 7일

// Refresh 토큰을 Redis에 저장 (userId → refreshToken)
redis.opsForValue().set("refresh:" + userId, refreshToken, Duration.ofDays(7));
```

**왜 refresh 토큰을 Redis에 저장하나?**
→ 서버가 refresh 토큰을 **폐기/회수**할 수 있어야 하기 때문. (JWT는 그 자체론 무효화 불가)

---

## 5. 매 요청 인증 (JwtAuthenticationFilter)

모든 요청은 `JwtAuthenticationFilter`를 통과한다.

```java
protected void doFilterInternal(...) {
    String token = extractToken(request);  // "Bearer xxx"

    if (token != null && tokenProvider.isTokenValid(token)) {
        Claims claims = validate(token);
        Long userId = getUserId(claims);

        // ⭐ 강제 로그아웃 여부 확인
        if (redis.hasKey("revoked:" + userId)) {
            // 차단된 사용자 → 인증 거부
            SecurityContextHolder.clearContext();
            return;
        }

        // ⭐ role 조회 (Redis 캐시 → DB → JWT 폴백)
        String role = lookupRole(userId, claims);

        // SecurityContext에 인증 정보 저장
        setAuthentication(userId, loginId, email, role);
    }
    filterChain.doFilter(request, response);
}
```

---

## 6. Refresh (재발급) 플로우

`POST /api/auth/refresh`

```java
public AuthToken refresh(String refreshToken) {
    // 1. JWT 서명/만료 검증
    // 2. Redis에 저장된 refresh 토큰과 일치하는지 확인
    String stored = redis.get("refresh:" + userId);
    if (stored == null || !stored.equals(refreshToken))
        throw TOKEN_REVOKED;

    // 3. 기존 토큰 폐기 후 새 토큰 발급 (Refresh Token Rotation)
    redis.delete("refresh:" + userId);
    return issueTokens(user);
}
```

**⭐ Refresh Token Rotation**: 재발급할 때마다 refresh 토큰을 **교체**한다. 탈취된 refresh 토큰이 재사용되는 것을 방지.

---

## 7. 로그아웃 & 강제 로그아웃

### 일반 로그아웃
```java
public void logout(Long userId) {
    redis.delete("refresh:" + userId);  // refresh 토큰 삭제
}
```

### 관리자 강제 로그아웃 (⭐ 핵심 기능)
관리자가 Redis 탭에서 사용자 세션을 강제 종료:

```java
public void deleteRefreshToken(Long userId) {
    redis.delete("refresh:" + userId);              // 1. refresh 삭제
    // 2. 이미 발급된 access token도 차단
    redis.set("revoked:" + userId, "1", 15분);      // access 토큰 만료시간과 동일
}
```

**문제**: JWT access token은 서버에 저장 안 되므로, refresh를 지워도 **이미 발급된 access token(15분)은 계속 유효**하다.

**해결**: `revoked:{userId}` 키를 두고, JWT 필터가 매 요청마다 이 키를 확인한다. TTL을 access token 수명(15분)과 맞춰서, 15분이 지나면 자연 소멸.

---

## 8. Role 캐시 (JWT stale role 문제)

**발생했던 실제 버그**:
1. 사용자 역할을 USER → ADMIN으로 DB에서 변경
2. 하지만 이미 발급된 JWT에는 `role: "USER"` 가 박혀있음
3. 프론트는 DB 기반 `/auth/me`로 ADMIN 확인 → 관리자 링크 보임
4. 하지만 백엔드 `@PreAuthorize`는 JWT의 role(USER)을 씀 → **접근 거부**

**해결**: role을 JWT가 아니라 **DB에서 조회** (Redis 캐시)

```java
private String lookupRole(Long userId, Claims claims) {
    // 1. Redis 캐시 확인
    String cached = redis.get("role:" + userId);
    if (cached != null) return cached;

    // 2. DB 조회 (1시간 캐시)
    String role = userMapper.findById(userId)
        .map(User::getRole)
        .orElse(claims.get("role", String.class));  // JWT 폴백
    redis.set("role:" + userId, role, 1시간);
    return role;
}
```

역할 변경 시 캐시 무효화:
```java
redis.delete("role:" + userId);  // AdminService.updateUserRole()
```

**면접 포인트**: JWT의 claims는 **발급 시점의 스냅샷**이라 실시간 변경 사항을 반영 못 한다. 자주 바뀌는 정보(role)는 DB/캐시에서 조회하는 게 안전하다.

---

## 9. Rate Limiting (고정 윈도우)

`RateLimitFilter`가 특정 경로의 요청 빈도를 제한:

| 경로 | 제한 |
|------|------|
| `/api/auth/login` | IP당 5회 / 5분 |
| `/api/auth/register` | IP당 3회 / 10분 |
| `/api/domains/search` | IP당 30회 / 1분 |

```java
String key = "rl:" + path + ":" + ip;
Long count = redis.opsForValue().increment(key);  // 카운트 증가
if (count == 1) redis.expire(key, window);        // 첫 요청에 TTL 설정
if (count > maxRequests) return 429;              // 초과 시 차단
```

**왜 Redis인가?** 다중 서버에서도 **공유 카운터**가 필요하기 때문. (서버 로컬 메모리면 서버마다 카운트가 따로 놈)

**한계 (면접 포인트)**: 고정 윈도우는 경계 시점(예: 4분 59초)에 요청이 몰리면 우회될 수 있다. 보완책은 **슬라이딩 윈도우** 또는 **토큰 버킷** 알고리즘.

---

## 10. 비밀번호 해싱 & 계정 보안

- **BCrypt(12)**: 비밀번호를 단방향 해시로 저장. salt가 자동 포함되어 동일 비밀번호도 매번 다른 해시.
- **로그인 실패 잠금**: 5회 실패 시 15분 계정 잠금 (`locked_until` 컬럼)
- **이메일 인증**: 가입 후 이메일 인증 완료해야 로그인 가능
- **비밀번호 재설정**: 이메일 링크 토큰, Redis에 15분 저장

---

## 11. Redis 키 구조 총정리

| 키 패턴 | 용도 | TTL |
|---------|------|-----|
| `refresh:{userId}` | refresh 토큰 저장 | 7일 |
| `revoked:{userId}` | 강제 로그아웃된 access token 차단 | 15분 |
| `role:{userId}` | 사용자 role 캐시 | 1시간 |
| `rl:{path}:{ip}` | rate limit 카운터 | 경로별 (1~10분) |
| `verify_token:{token}` | 이메일 인증 토큰 | 24시간 |
| `reset_token:{token}` | 비밀번호 재설정 토큰 | 15분 |
| `lock:domain:{punycode}` | 서브도메인 등록 분산 락 | 10초 |

---

## 12. 면접 예상 질문 & 답변 포인트

### Q1. "JWT를 쓰면 로그아웃이 안 되는데 어떻게 처리했나요?"
→ access token은 15분으로 짧게 + `revoked:{userId}` Redis 키로 즉시 차단. refresh token은 Redis에서 삭제.

### Q2. "Access/Refresh 토큰을 왜 나눴나요?"
→ access는 짧게(15분) 해서 탈취 피해 최소화, refresh는 Redis에서 제어해 즉시 폐기 가능. 보안과 UX의 균형.

### Q3. "JWT의 단점은?"
→ (1) 발급 후 서버에서 무효화 어려움 → revoked 키로 보완, (2) claims 스냅샷이라 실시간 반영 안 됨 → role은 DB 조회, (3) 토큰 크기 증가 → 최소 claims만 담음.

### Q4. "세션 방식 대신 JWT를 쓴 이유는?"
→ 무상태 서버로 **수평 확장** 용이. 서버가 세션을 메모리에 안 가지므로 아무 서버나 요청 처리 가능. 세션 동기화(스티키 세션) 불필요.

### Q5. "Refresh token이 탈취되면?"
→ Refresh Token Rotation(재발급 시 교체)으로 기존 토큰 무효화. 다만 완벽한 방어는 어려우므로, 이상 징후(같은 refresh 토큰 재사용) 감지 시 전체 세션 폐기가 보강책.

### Q6. "BCrypt는 왜 쓰고, 왜 12인가?"
→ 단방향 해시 + 자동 salt. cost factor 12는 보안성과 성능의 균형점(해시 1회에 수백 ms). 너무 높으면 로그인마다 CPU 부담.

### Q7. "rate limiting을 로컬 메모리가 아닌 Redis로 한 이유는?"
→ 다중 서버 환경에서도 전역 카운터 공유가 필요. 로컬이면 서버마다 한도가 따로 적용돼 우회 가능.
