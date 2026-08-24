# Web Domain Registry

도메인 DNS 레코드 관리 플랫폼 — 서브도메인, A/AAAA/CNAME/MX/TXT/NS 레코드를 웹에서 관리합니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.4, Maven 멀티모듈 |
| Security | Spring Security + JWT |
| Database | PostgreSQL 16 + MyBatis + Flyway |
| Cache | Redis 7 |
| Frontend | Vue 3 + Vite + TypeScript + Tailwind CSS |

## 프로젝트 구조

```
web-domain-reg/
  pom.xml
  docker-compose.yml
  services/api/                    # Spring Boot
  modules/
    domain-core/                   # Entity, Port, VO
    domain-persistence/            # MyBatis, Flyway, Repository
    domain-registrar/              # DNS Provider Client
    domain-payment/                # Payment Gateway
  apps/web/                        # Vue 3 SPA
```

## 빠른 시작

### 사전 요구사항
- Java 21 + Maven 3.9+
- Node.js 22+
- PostgreSQL 16 + Redis 7

### 백엔드

```bash
cd web-domain-reg
export DB_HOST=217.142.144.114 DB_USER=postgreuser DB_PASS='<DB_PASSWORD>' DB_SCHEMA=domaindb
mvn clean package -pl services/api -am
java -jar services/api/target/api-0.1.0-SNAPSHOT.jar
# → http://localhost:8080
```

### 프론트엔드

```bash
cd apps/web
npm install && npm run dev
# → http://localhost:5173
```

## API

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/auth/register` | - | 회원가입 |
| POST | `/api/auth/login` | - | 로그인 |
| GET | `/api/me/domains` | JWT | 내 도메인 목록 |
| GET | `/api/domains/{id}` | JWT | 도메인 상세 |
| GET | `/api/domains/{id}/dns` | JWT | DNS 레코드 목록 |
| POST | `/api/domains/{id}/dns` | JWT | DNS 레코드 추가 |
| PUT | `/api/domains/{id}/dns/{recordId}` | JWT | DNS 레코드 수정 |
| DELETE | `/api/domains/{id}/dns/{recordId}` | JWT | DNS 레코드 삭제 |
| PUT | `/api/domains/{id}/nameservers` | JWT+paid | NS 변경 |
| PUT | `/api/domains/{id}/privacy` | JWT+paid | Privacy 토글 |
| POST | `/api/orders` | JWT | 주문 |
| POST | `/api/payments/confirm` | JWT | 결제 |

## DB 연결

```
Host: 217.142.144.114:5432  Database: postgres  Schema: domaindb
User: postgreuser            Password: <DB_PASSWORD>
```

## 개발 현황

| Phase | 내용 | 상태 |
|-------|------|------|
| Phase 0 | 골격 | ✅ |
| Phase 1 | 결제·등록 | ✅ |
| Phase 2 | NS/Privacy 관리 | ✅ |
| Phase 3 | DNS 레코드 관리 | 🚧 |
| Phase 4 | 고도화 | ⬜ |
