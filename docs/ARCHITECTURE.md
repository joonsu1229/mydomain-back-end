# 🗺️ 도메인온(DomainOn) 전체 구조 지도

> 기술 면접 대비용 — 프로젝트의 전체 아키텍처, 설계 원칙, 기술 선택 이유를 정리한 문서입니다.

---

## 1. 프로젝트 한 줄 소개

**도메인온**은 관리자가 루트 도메인(예: `domon.kr`)을 구매해 플랫폼에 등록하고, 일반 사용자가 그 아래에 서브도메인(예: `myblog.domon.kr`)을 발급받는 **서브도메인 발급 플랫폼**이다.

핵심 기능:
- 회원가입 / 로그인 (JWT + Redis 세션)
- 이메일 인증 (Naver SMTP)
- 서브도메인 발급 (무료, 즉시 활성)
- 관리자 콘솔 (사용자/도메인/플랫폼도메인/Redis 모니터링)
- 플랫폼 도메인 소유권 검증 (DNS TXT)
- 결제 연동 (Toss Payments, 현재 stub 모드)

---

## 2. 기술 스택

| 계층 | 기술 | 선택 이유 |
|------|------|-----------|
| Backend | Java 17 + Spring Boot 3.4 | 한국 도메인/레거시 생태계와 친숙, 성숙한 생태계 |
| DB | PostgreSQL 16 | JSONB, 타임존, 엄격한 무결성 |
| ORM | MyBatis 3 | SQL 직접 제어, 쿼리 튜닝 용이 (JPA보다 가벼움) |
| 캐시/세션 | Redis 7 | refresh 토큰, rate limit, 캐시, 분산 락 |
| 마이그레이션 | Flyway | 스키마 버전 관리 (현재 수동 적용 상태) |
| Frontend | Vue 3 + Pinia + TypeScript | Composition API, 타입 안정성 |
| 인증 | JWT (jjwt, HMAC-SHA256) | 무상태 인증 |
| 배포 | Docker Compose | Postgres + Redis + API 일괄 기동 |

---

## 3. 멀티모듈 구조 (Hexagonal Architecture)

```
web-domain-reg/
├── apps/
│   └── web/                     # Vue 3 프론트엔드
├── modules/
│   ├── domain-core/             # 순수 도메인 (엔티티, 포트, 핵심 서비스)
│   ├── domain-persistence/      # MyBatis 매퍼 + 리포지토리 구현체 (어댑터)
│   ├── domain-registrar/        # 외부 레지스트라 연동 (stub/partner)
│   └── domain-payment/          # Toss Payments 연동
└── services/
    └── api/                     # REST 컨트롤러, 보안, 애플리케이션 서비스
```

### 모듈별 책임

| 모듈 | 역할 | 핵심 내용 |
|------|------|-----------|
| `domain-core` | **도메인 로직의 중심** | `User`, `Domain`, `PlatformDomain`, `Order` 엔티티, `*Repository` 포트 인터페이스 |
| `domain-persistence` | DB 접근 (어댑터) | `UserMapper`, `DomainMapper` 등 + `*RepositoryImpl` |
| `domain-registrar` | 외부 도메인 등록기관 연동 | stub 구현 (현재) |
| `domain-payment` | 결제 연동 | Toss Payments (현재 stub) |
| `services/api` | 웹 계층 | 컨트롤러, `JwtAuthenticationFilter`, 서비스 조립 |

### 🔑 핵심 설계 원칙: **포트-어댑터 (Hexagonal)**

- **포트(Port)**: `domain-core`에 정의된 인터페이스 (`UserRepository`, `DomainRepository`)
- **어댑터(Adapter)**: `domain-persistence`에 구현 (`UserRepositoryImpl`)
- **의존성 방향**: `services/api` → `domain-core` ← `domain-persistence`

**왜 이렇게 나눴는가 (면접 포인트):**
1. 도메인 로직이 DB/프레임워크에 **의존하지 않는다** → 테스트가 쉽다.
2. DB를 PostgreSQL → 다른 DB로 바꿔도 `domain-core`는 **수정 없음**.
3. 레지스트라/결제 같은 **외부 서비스 교체**가 용이하다 (stub → 실제).

---

## 4. 시스템 구성도

```
                        ┌──────────────────────────────┐
                        │        Vue 3 (브라우저)        │
                        │   Pinia (auth store)          │
                        └──────────────┬───────────────┘
                                       │ HTTP (JSON) / JWT
                        ┌──────────────▼───────────────┐
                        │   Spring Boot API (:8080)     │
                        │  ┌─────────────────────────┐ │
                        │  │ Security Filter Chain    │ │
                        │  │ 1. RateLimitFilter       │ │
                        │  │ 2. JwtAuthenticationFilter│ │
                        │  │ 3. @PreAuthorize (role)  │ │
                        │  └─────────────────────────┘ │
                        │  ┌─────────────────────────┐ │
                        │  │ Controller → Service     │ │
                        │  │ → Port(interface)        │ │
                        │  └─────────────────────────┘ │
                        └──────┬────────────────┬──────┘
                               │                │
                    ┌──────────▼─────┐   ┌──────▼─────────┐
                    │  PostgreSQL 16 │   │    Redis 7      │
                    │  (영구 데이터)   │   │ (세션/캐시/락)   │
                    └────────────────┘   └────────────────┘
```

---

## 5. 요청 처리 흐름 (예: 로그인된 사용자가 서브도메인 발급)

```
1. 클라이언트 → POST /api/orders (Bearer accessToken)
2. RateLimitFilter   → 요청 경로가 rate limit 대상인지 확인 (여기선 아님)
3. JwtAuthenticationFilter
   ├─ Authorization 헤더에서 토큰 추출
   ├─ 서명/만료 검증 (jjwt)
   ├─ revoked:{userId} 키 확인 (강제 로그아웃 여부)
   └─ role 조회 (Redis 캐시 → DB → JWT 폴백) → SecurityContext 설정
4. Spring Security → @PreAuthorize 평가 (인증된 사용자만)
5. OrderController.createOrder()
6. OrderService.createOrder()
   ├─ Redis SETNX 분산 락 획득 (lock:domain:{punycode})
   ├─ DB 중복 체크 (existsByPunycode)
   ├─ Domain 생성 + 저장 (MyBatis INSERT)
   └─ Order 생성 + 저장
7. 응답 반환
```

---

## 6. 핵심 기술 선택 이유 (면접에서 자주 물어봄)

### Q. 왜 JPA가 아니라 MyBatis를 썼나요?
- SQL을 **직접 제어**하고 싶었고, 복잡한 조인/집계 쿼리 튜닝이 용이하다.
- 도메인 주도 설계에서 **리포지토리 패턴**을 포트-어댑터로 구현할 때 매퍼가 더 직관적이었다.
- (단점 인지) 보일러플레이트가 많고, 엔티티-매핑을 수동 관리해야 한다.

### Q. 왜 Redis를 도입했나요?
1. **무상태(stateless) 서버**의 수평 확장을 위해 refresh 토큰을 Redis에 저장
2. 로그인/회원가입/검색 **rate limiting** (고정 윈도우)
3. 사용자 role **캐시** (DB 조회 부담 감소)
4. 강제 로그아웃을 위한 **access token 차단**(revoked 키)
5. 서브도메인 중복 등록 방지를 위한 **분산 락**

### Q. 헥사고날 아키텍처의 장단점은?
- **장점**: 도메인 격리, 테스트 용이, 인프라 교체 자유로움
- **단점**: 초기 보일러플레이트 증가, 작은 프로젝트에선 과할 수 있음 (이 프로젝트에서도 이 점은 인정)

---

## 7. 배포 & 인프라

- `docker-compose.yml`: PostgreSQL + Redis + API 3개 컨테이너
- Redis: `--appendonly yes` (AOF 영속화)
- 환경변수로 DB/Redis/JWT/SMTP/Toss 설정 주입 (`${DB_HOST}`, `${JWT_SECRET}` 등)
- 프론트엔드: Vite 빌드, hash 라우팅 (리버스 프록시 하에서도 라우팅 깨지지 않음)

---

## 8. 면접 예상 질문 TOP 10

1. 프로젝트 전체 구조를 설명해보세요.
2. 헥사고날(포트-어댑터) 아키텍처를 선택한 이유는?
3. Redis를 어디에 쓰고, 각각의 TTL 정책은?
4. 무상태 서버에서 로그인 세션을 어떻게 유지하나요?
5. JWT의 단점은 무엇이고 어떻게 보완했나요?
6. 데이터베이스 스키마는 어떻게 설계했나요? (관계: users ↔ domains ↔ orders)
7. 동시에 같은 서브도메인을 등록하면 어떻게 되나요? (분산 락)
8. 관리자 권한은 어떻게 분리했나요? (@PreAuthorize + role 캐시)
9. 서버를 여러 대로 늘리면(수평 확장) 무엇을 바꿔야 하나요?
10. 운영 배포 시 보안적으로 무엇을 보강해야 하나요?
