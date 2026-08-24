# 🌐 도메인 등록 플로우

> 기술 면접 대비용 — 플랫폼 도메인 관리, 서브도메인 발급, DNS 검증, 분산 락까지의 전체 흐름을 정리한 문서입니다.

---

## 1. 도메인 개념 정리

이 플랫폼에는 **두 종류의 도메인**이 있다.

| 종류 | 주체 | 예시 | 설명 |
|------|------|------|------|
| **플랫폼 도메인** | 관리자 | `domon.kr` | 관리자가 실제로 구매한 루트 도메인 |
| **서브도메인** | 일반 사용자 | `myblog.domon.kr` | 플랫폼 도메인 아래에 사용자가 발급 |

```
domon.kr              ← 플랫폼 도메인 (관리자가 소유, DNS TXT로 검증)
├── myblog.domon.kr   ← 사용자 A의 서브도메인
├── shop.domon.kr     ← 사용자 B의 서브도메인
└── dev.domon.kr      ← 사용자 C의 서브도메인
```

---

## 2. 전체 흐름 요약

```
[관리자] 플랫폼 도메인 등록 → DNS TXT 소유권 검증 → 활성화
                                              ↓
[사용자] 플랫폼 도메인 선택 → 서브도메인명 입력 → 분산 락 → 중복 체크 → 발급
                                              ↓
[사용자] 자기 도메인에 A/MX/TXT 레코드 등록 → DB 저장 → 비동기 동기화(잡 큐)
```

---

## 3. 플랫폼 도메인 등록 (관리자)

### 3-1. 등록 (PENDING 상태)

관리자가 `POST /admin/platform-domains` 로 도메인 등록:

```java
public PlatformDomain addPlatformDomain(String domainName, ...) {
    // 1. 소문자 변환 + IDN 변환
    String punycode = IDN.toASCII(trimmed);   // "도메인.한국" → "xn--..."

    // 2. 중복 체크
    if (existsByPunycode(punycode)) throw DUPLICATE;

    // 3. 검증 토큰 생성 + PENDING 상태로 저장
    String token = UUID.randomUUID();  // 랜덤 토큰
    pd.setStatus("PENDING");
    pd.setVerificationToken(token);
    save(pd);
}
```

**왜 PENDING 상태가 필요한가?**
→ 관리자가 **실제로 소유한 도메인인지 검증**해야 한다. 아무 도메인이나 등록하면 남의 도메인을 탈취한 것처럼 보일 수 있으므로.

### 3-2. DNS TXT 소유권 검증 (⭐ 핵심)

`POST /admin/platform-domains/{id}/verify`

**원리**: 도메인을 진짜 소유한 사람만이 그 도메인의 DNS 레코드를 수정할 수 있다는 점을 이용.

1. 관리자가 DNS 설정에 TXT 레코드 추가:
   ```
   호스트: _domainon.domon.kr
   값:     domainon-verify={랜덤토큰}
   ```

2. 서버가 DNS 조회 (Google DNS 8.8.8.8):
```java
String host = "_domainon." + pd.getNameUnicode();
List<String> txtRecords = dnsLookup.lookupTxt(host);  // JNDI DNS 조회

String expected = "domainon-verify=" + pd.getVerificationToken();
boolean found = txtRecords.stream().anyMatch(r -> r.contains(expected));

if (!found) throw VERIFY_FAILED;  // TXT 없으면 거부
else pd.setStatus("ACTIVE");      // 검증 성공 → 활성화
```

**면접 포인트**:
- DNS 기반 소유권 검증은 Google, Let's Encrypt(DNS-01 챌린지)가 쓰는 표준 방식.
- `_domainon` 서브도메인을 써서 메인 도메인 DNS와 충돌 없이 별도 레코드로 관리.
- DNS 전파(propagation) 때문에 추가 직후엔 조회가 안 될 수 있음 → "최대 5분 소요" 안내.

### 3-3. 도메인 수명주기 (3가지 상태)

| 상태 | 의미 | 사용자 노출 |
|------|------|-------------|
| `ACTIVE` (활성) | 검증 완료 + 활성화 | ✅ 노출 |
| `INACTIVE` (비활성) | 일시 중단 | ❌ 숨김 |
| `PENDING` (인증 대기) | 아직 검증 전 | ❌ 숨김 |

**만료일(expiresAt) 체크**: 도메인 만료일이 지나면 사용자가 실제로 못 쓰므로, 활성화 시 만료 여부를 검사한다.

```java
public PlatformDomain activatePlatformDomain(Long id) {
    // 만료 체크
    if (pd.getExpiresAt() != null && pd.getExpiresAt().isBefore(now))
        throw DOMAIN_EXPIRED;  // 만료된 도메인은 활성화 불가
    pd.setActive(true);
}
```

조회 쿼리에서도 만료 도메인 제외:
```sql
WHERE (expires_at IS NULL OR expires_at > NOW())
```

---

## 4. 서브도메인 발급 (사용자)

### 4-1. 플로우

`POST /api/orders` → `OrderService.createOrder()`

```java
public Order createOrder(Long userId, Long platformDomainId, String prefix) {
    // 1. prefix 검증 (소문자+숫자+하이픈)
    if (!prefix.matches("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$"))
        throw INVALID_PREFIX;

    // 2. 플랫폼 도메인 로드 + 활성 여부 확인
    PlatformDomain pd = findById(platformDomainId);
    if (!pd.isActive()) throw PLATFORM_INACTIVE;

    // 3. 전체 도메인명 조합 + punycode 변환
    String full = prefix + "." + pd.getNameUnicode();  // "myblog.domon.kr"
    IDN.toASCII(full);

    // 4. ⭐ 분산 락 획득 (중복 등록 방지)
    String lockKey = "lock:domain:" + fullPunycode;
    Boolean locked = redis.setIfAbsent(lockKey, "1", 10초);  // SETNX
    if (!locked) throw "이미 등록 중인 도메인입니다";

    try {
        // 5. DB 중복 체크
        if (existsByPunycode(fullPunycode)) throw DOMAIN_TAKEN;

        // 6. 도메인 생성 (ACTIVE) + 저장
        Domain domain = Domain.createFreeSubdomain(...);
        domainRepository.save(domain);

        // 7. 주문(Order) 생성 (현재 무료 = 0원)
        Order order = Order.create(...);
        orderRepository.save(order);
    } finally {
        redis.delete(lockKey);  // 8. 락 해제
    }
}
```

---

## 5. 분산 락 (⭐ 핵심 개념)

### 문제 상황

두 사용자가 **동시에** `myblog.domon.kr` 을 등록하려 하면?

```
스레드 A: existsByPunycode("myblog") → false (아직 없음)
스레드 B: existsByPunycode("myblog") → false (아직 없음)  ← 둘 다 없다고 봄
스레드 A: INSERT myblog
스레드 B: INSERT myblog  ← 중복 삽입 or 에러
```

DB unique 제약으로 막을 수는 있지만, **레코드가 이미 생성돼서 예외 처리**가 지저분해진다.

### 해결: Redis SETNX (SET if Not eXists)

```java
// 원자적 연산: 키가 없을 때만 값을 설정하고 true 반환
Boolean locked = redis.opsForValue()
    .setIfAbsent("lock:domain:" + fullPunycode, "1", Duration.ofSeconds(10));
```

- **SETNX는 원자적(atomic)** → 두 요청 중 하나만 `true`를 받는다.
- 락을 얻은 쪽만 중복 체크 + 저장 진행.
- 락을 못 얻은 쪽은 즉시 "이미 등록 중" 에러.
- **TTL 10초**로 데드락 방지 (서버가 죽어도 락이 자동 해제).

**면접 포인트**:
- Redis의 단일 스레드 특성 + SETNX의 원자성으로 분산 락 구현.
- TTL 없이 락을 걸면, 락 획득한 서버가 죽었을 때 **영원히 잠기는** 문제 → TTL 필수.
- 더 견고하게는 Redisson의 **Watchdog**(락 자동 연장)을 쓸 수 있음.

---

## 6. IDN / Punycode

한국어 도메인(`도메인.한국`)은 DNS에서 처리 못 하므로 ASCII로 변환해야 한다.

```java
IDN.toASCII("도메인.한국")  // → "xn--..."
IDN.toUnicode("xn--...")   // → "도메인.한국"
```

- DB에는 **두 가지 모두 저장**: `nameUnicode`(표시용) + `namePunycode`(DNS/중복체크용)
- 중복 체크는 항상 **punycode 기준** (대소문자/유니코드 정규화 차이 방지)

---

## 7. 데이터 모델 관계

```
users (1) ──────── (N) domains (N) ──────── (1) platform_domains
  │                      │
  └────── (N) orders ────┘
```

| 엔티티 | 주요 컬럼 | 설명 |
|--------|-----------|------|
| `users` | login_id, email, password_hash, role, email_verified | 회원 |
| `platform_domains` | name_unicode, name_punycode, status, verification_token, expires_at | 루트 도메인 |
| `domains` | user_id, platform_domain_id, name_unicode, name_punycode, status, expires_at | 서브도메인 |
| `orders` | user_id, domain_id, order_number, amount, status | 주문/결제 |
| `dns_records` | domain_id, record_type, name, content, ttl, priority | 사용자 DNS 레코드 (A/MX/TXT 등) |
| `domain_nameservers` | domain_id, host, ip, sort_order | NS delegation (네임서버 지정) |
| `registrar_jobs` | domain_id, job_type, status, attempts, next_retry_at | 비동기 동기화 잡 큐 |

---

## 8. DNS 레코드 동기화 (사용자 A/MX/TXT 등록)

사용자는 자기 도메인에 **A / AAAA / CNAME / MX / TXT / NS / SRV** 레코드를 등록할 수 있다. 이 레코드가 **실제 DNS 세계에 어떻게 반영되는지**가 이 섹션의 주제.

### 8-1. 개념: "레코드가 등록된다"는 것의 실체

DNS 레코드는 중앙 저장소가 아니라, **각 도메인의 authoritative(권한) 네임서버가 가진 zone 파일**에 한 줄 추가되는 것이다.

```
브라우저 → 로컬 resolver → root NS → .kr TLD NS → domon.kr의 NS(authoritative) → zone 파일에서 A 레코드 응답
```

→ `myblog.domon.kr`이 실제 응답되려면, `domon.kr` zone을 관리하는 네임서버의 zone 파일에 그 레코드가 있어야 한다.

레코드를 "실제로 등록"하는 3가지 방법:

| 방식 | 레코드를 쓰는 주체 | 비고 |
|------|--------------------|------|
| **① DNS 제공자 API** | Cloudflare / Route53 API 호출 | 쉽고 안정적, 무료 티어 있음 ⭐운영 추천 |
| **② 자체 DNS 서버** | PowerDNS/BIND + DB backend (INSERT 한 줄 = 레코드 등록) | 완전 통제, but DNS 가용성 책임 |
| **③ 와일드카드** | `*.domon.kr → 서버 IP` 한 줄만 등록 | 레코드별 제어 불가 |

### 8-2. 이 프로젝트의 구현: "DB가 source of truth + 비동기 동기화"

```
사용자 A/MX/TXT 추가
   ↓
DnsRecordController.create   (POST /api/domains/{domainId}/dns)
   ↓
DnsRecordService.addRecord()
   ├─ 타입 검증 (A/AAAA/CNAME/MX/TXT/NS/SRV)
   ├─ dns_records 테이블 INSERT   ← source of truth는 DB
   └─ enqueueSyncJob(): SYNC_DNS 잡을 registrar_jobs 큐에 저장
        ↓
RegistrarJobWorker (@Scheduled 5초마다 폴링)
   ├─ pending 잡 → PROCESSING
   ├─ registrarClient.syncDnsRecords(zoneName, domainName, records)   ← ⭐ 실제 등록 지점
   ├─ 성공 → COMPLETED / 실패 → 지수 백오프 재시도(2s→4s→8s→16s) → DEAD
```

핵심 코드 (`RegistrarJobWorker.handleSyncDns`):

```java
String zoneName = resolveZoneName(domain);   // 서브도메인 → 플랫폼 도메인 이름 = Cloudflare zone
List<DnsRecord> records = dnsRecordMapper.findByDomainId(domain.getId());
registrarClient.syncDnsRecords(zoneName, domain.getNameUnicode(), records);
```

**설계 포인트 (면접 강조)**:
1. **DB가 source of truth** — 레코드는 항상 `dns_records`에 저장되고, 외부 DNS 반영은 "동기화"로 취급.
2. **비동기 + 재시도 (outbox / job-queue 패턴)** — DNS 제공자 API가 느리거나 실패해도 사용자 요청은 즉시 응답, 실패는 백그라운드 재시도.
3. **포트-어댑터** — `RegistrarClient` 인터페이스로 DNS 제공자를 교체 가능.
4. **zone 해석** — 서브도메인의 DNS는 자기 플랫폼 도메인의 zone에 들어간다. (`myblog.domon.kr` → zone `domon.kr`)

### 8-3. stub이란? (⭐)

**stub(스텁)** = 실제 외부 시스템(여기선 DNS 제공자 API) 대신, **"흉내만 내는 가짜 구현"**.

```java
// StubRegistrarClient — 실제로는 아무것도 안 함
public void syncDnsRecords(String zoneName, String domainName, List<DnsRecord> records) {
    simulateLatency();   // 가짜 지연
    // no-op — 실제 DNS 쓰기 없음. DB에만 기록됨
}
```

- 개발/테스트 단계에서 외부 결제·레지스트라 없이 전체 흐름을 돌려보기 위해 쓴다.
- 실제 운영에서는 `app.registrar.mode=partner`로 설정 → `PartnerRegistrarClient`가 Cloudflare API로 실제 반영한다. (아래 8-5)

### 8-4. RegistrarClient 포트의 두 구현

| 구현 | 활성 조건 | 동작 |
|------|-----------|------|
| `StubRegistrarClient` | `app.registrar.mode=stub` (기본값) | no-op, 가짜 지연만 |
| `PartnerRegistrarClient` | `app.registrar.mode=partner` | ✅ Cloudflare API 연동 (실제 DNS 반영) |

### 8-5. Cloudflare 연동 실제 구현 (⭐)

`PartnerRegistrarClient`가 Cloudflare v4 API를 호출해 **레코드를 실제 DNS에 반영**한다.

**① zone 해석**: 서브도메인의 플랫폼 도메인 이름이 곧 Cloudflare zone 이름이다.

```
myblog.domon.kr → platform_domain = domon.kr → Cloudflare zone "domon.kr"
```

`RegistrarJobWorker.resolveZoneName()`이 `platformDomainId`를 보고 zone 이름을 찾아 넘긴다.

**② reconciliation (3-way 동기화)**: DB(`dns_records`)가 source of truth.

```
[Cloudflare 기존 레코드]  vs  [DB의 원하는 레코드]
        ├─ DB에 있고 Cloudflare에 없음          → create
        ├─ 둘 다 있고 내용 다름                 → update
        └─ 우리가 관리하던 것인데 DB에서 삭제됨  → delete
```

**③ 안전장치 — comment 태그**: 우리가 만든 레코드에 `comment = "domainon:{도메인명}"`을 달아둔다. 동기화할 때 **이 태그가 붙은 레코드만** create/update/delete 하므로, Cloudflare가 자동으로 만든 레코드나 남이 만든 레코드는 절대 건드리지 않는다.

**④ 지원 레코드**: A / AAAA / CNAME / MX / TXT / NS. (SRV는 Cloudflare가 `data` 객체를 요구해서 현재 미지원 — warning 로그 후 skip)

**⑤ 설정 (`application.yml`)**:

```yaml
app:
  registrar:
    mode: partner                        # REGISTRAR_MODE=partner
  cloudflare:
    api-token: ${CLOUDFLARE_API_TOKEN:}  # Zone:DNS Edit 권한 API 토큰
    base-url: ${CLOUDFLARE_BASE_URL:https://api.cloudflare.com/client/v4}
```

### 8-6. 두 종류의 레코드 (헷갈리지 말 것)

| 테이블 | 담는 것 | 동기화 잡 |
|--------|---------|-----------|
| `domain_nameservers` | NS delegation (ns1.domon.kr 등) | `UPDATE_NS` |
| `dns_records` | 사용자 A/AAAA/CNAME/MX/TXT/SRV | `SYNC_DNS` |

---

## 9. 결제 연동 (현재 stub)

- 현재는 **stub 모드**: 실제 결제 없이 0원 주문 생성.
- 구조상 `domain-payment` 모듈에 Toss Payments 클라이언트가 있고, `REGISTRAR_MODE=stub` / `PAYMENT` 설정으로 실제 연동으로 교체 가능.
- (포트-어댑터 덕분에 stub → 실제 결제 교체가 용이)

---

## 10. 면접 예상 질문 & 답변 포인트

### Q1. "도메인이 실제 사용자의 것인지 어떻게 검증하나요?"
→ **DNS TXT 레코드** 검증. `_domainon.{domain}` 에 특정 토큰을 TXT로 넣게 하고, 서버가 DNS 조회로 확인. DNS를 수정할 수 있는 사람 = 도메인 소유자라는 원리.

### Q2. "동시에 같은 서브도메인을 등록하면?"
→ **Redis SETNX 분산 락**으로 직렬화. 하나만 락 획득, 나머지는 거부. TTL로 데드락 방지.

### Q3. "왜 DB unique 제약만으로 충분하지 않나요?"
→ unique 제약은 최종 방어선으로 두고, 락으로 **먼저 막는 것**이 UX가 낫다(깔끔한 에러 메시지). 또 락은 "중복 체크 + 저장"을 원자적으로 묶어 레이스 컨디션을 원천 차단.

### Q4. "IDN/punycode 변환을 왜 하나요?"
→ DNS는 ASCII만 처리 가능. 한글 도메인을 저장/조회하려면 punycode로 변환해야 하고, 중복 체크 정확성을 위해 punycode 기준으로 비교.

### Q5. "도메인 만료는 어떻게 처리하나요?"
→ 플랫폼 도메인에 `expires_at`을 두고, 활성화 시 만료 여부 검사 + 조회 쿼리에서 만료 도메인 제외(`expires_at > NOW()`).

### Q6. "이 구조에서 실제 도메인 등록기관(레지스트라) 연동은 어디에?"
→ `domain-registrar` 모듈. 현재 stub이지만 포트-어댑터 구조라 실제 레지스트라 API로 교체 가능.

### Q7. "서브도메인은 실제로 DNS에 어떻게 반영되나요?"
→ 현재는 DB에만 저장되고 즉시 ACTIVE. 실제 운영에서는 와일드카드 DNS(`*.domon.kr` → 서버 IP)를 걸고, 서버가 도메인명으로 요청을 라우팅하는 방식으로 확장할 수 있다.

### Q8. "사용자가 등록한 A/MX/TXT 레코드는 실제로 어떻게 반영되나요?"
→ DB(`dns_records`)에 즉시 저장하고, `RegistrarJobWorker`가 5초마다 큐를 폴링해 `RegistrarClient`(포트)를 통해 Cloudflare API로 비동기 반영한다. Cloudflare zone은 서브도메인의 플랫폼 도메인 이름으로 해석하고, comment 태그로 우리가 관리하는 레코드만 3-way reconciliation(create/update/delete) 한다. 실패 시 지수 백오프 재시도.

### Q9. "stub이 무엇이고, 왜 쓰나요?"
→ 실제 외부 시스템(결제/레지스트라/DNS) 대신 흉내만 내는 가짜 구현. 개발·테스트 단계에서 외부 의존 없이 전체 플로우를 검증하기 위해 쓰고, 포트-어댑터 구조 덕분에 실운영 시 실제 구현으로 갈아끼울 수 있다.
