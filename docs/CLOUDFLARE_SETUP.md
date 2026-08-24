# ☁️ Cloudflare 연동 — 세팅 & 사용법

> 도메인온을 **실제 DNS에 붙이기 위한** 셋업 가이드. 도메인을 사서 Cloudflare에 연결하고, 플랫폼 도메인을 등록하고, 사용자 레코드가 실제로 반영되는지 확인하는 전체 과정.

---

## 1. 개요 (무슨 일이 일어나는가)

`app.registrar.mode=partner` 로 켜면, 사용자가 등록한 A/MX/TXT 레코드가 **Cloudflare DNS로 실제 반영**된다.

```
사용자 DNS 레코드 추가 → DB 저장 → (5초) RegistrarJobWorker → Cloudflare API → 실제 DNS 반영
```

- **zone 해석**: 서브도메인 `myblog.domon.kr` → 플랫폼 도메인 `domon.kr` → Cloudflare zone `domon.kr`
- **안전장치**: 우리가 만든 레코드에 `comment: "domainon:{도메인명}"` 태그를 달아, 그 태그가 붙은 레코드만 관리

---

## 2. 사전 준비물

| 항목 | 설명 |
|------|------|
| 루트 도메인 | 예: `domon.kr` (어느 레지스트라에서 구매해도 무방) |
| Cloudflare 계정 | 무료(Free) 플랜으로 충분 |

---

## 3. Cloudflare 설정 (1회)

### 3-1. 사이트(zone) 추가

1. Cloudflare 로그인 → **"Add a site"** → 도메인 입력 (`domon.kr`)
2. DNS 레코드 스캔 → Free 플랜 선택 → Continue
3. Cloudflare가 **네임서버 2개**를 알려줌 (예: `xxx.ns.cloudflare.com`, `yyy.ns.cloudflare.com`)

### 3-2. 네임서버(NS) 변경 — 루트 도메인 1회 셋업

도메인을 구매한 **레지스트라(가비아/후이즈 등) 사이트**에서, `domon.kr`의 네임서버를 위 2개의 Cloudflare 네임서버로 변경.

> ⚠️ **이건 "사용자 서브도메인의 네임서버 변경" 기능과 다르다.** 여기서 하는 건 `domon.kr` 존을 Cloudflare가 서빙하게 만드는 **1회성 플랫폼 셋업**일 뿐이다. 어느 DNS 제공자를 쓰든 "누군가 `domon.kr`의 권한 네임서버가 되어야" 서브도메인을 서빙하고 위임할 수 있으므로, 이 단계는 필수다.
>
> NS 전파는 보통 1시간 내, 최대 24~48시간 소요. Cloudflare 대시보드에서 "Active" 상태가 되면 완료.

### 3-3. API 토큰 발급

1. Cloudflare 대시보드 → **My Profile → API Tokens → Create Token**
2. **Create Custom Token** 선택
3. 권한 설정:
   - `Zone → Zone → Read`
   - `Zone → DNS → Edit`
4. Zone Resources: `Include → Specific zone → domon.kr`
5. 토큰 생성 후 **한 번만 표시되는 토큰 값 복사**

---

## 4. 앱 환경설정

### 4-1. 환경변수 (docker-compose / .env 권장)

```bash
REGISTRAR_MODE=partner            # stub → partner (Cloudflare 실연동)
CLOUDFLARE_API_TOKEN=발급받은토큰
PAYMENT_MODE=stub                 # 결제는 그대로 stub (기본값)
```

### 4-2. application.yml (직접 편집 시)

```yaml
app:
  registrar:
    mode: partner
  cloudflare:
    api-token: ${CLOUDFLARE_API_TOKEN:}
    base-url: ${CLOUDFLARE_BASE_URL:https://api.cloudflare.com/client/v4}
  payment:
    mode: ${PAYMENT_MODE:stub}
```

> ⚠️ `api-token`이 비어 있으면 `PartnerRegistrarClient`가 생성될 때 앱이 기동 실패한다.
> ⚠️ 결제(`PAYMENT_MODE`)는 레지스트라 모드와 **분리**되어 있으니 `partner`로 바꿔도 결제는 stub 유지하면 된다.

---

## 5. 앱에서 플랫폼 도메인 등록 + TXT 인증

1. 관리자 로그인 → 관리자 → **플랫폼 도메인 추가**
   - 도메인명: `domon.kr`, 표시명/설명 입력
   - 상태가 `PENDING`이 되고 **인증 토큰**이 발급됨
2. Cloudflare 대시보드에서 **TXT 레코드 추가**:
   - Type: `TXT`
   - Name: `_domainon`   (→ `_domainon.domon.kr`)
   - Content: `domainon-verify={토큰값}`
3. 앱에서 **"인증 확인"** 클릭 → 서버가 DNS 조회로 TXT 확인 → `ACTIVE` 전환
   - (DNS 전파 때문에 직후엔 실패할 수 있음 → 최대 5분 후 재시도)

---

## 6. 실제 사용 흐름

1. 사용자가 `myblog.domon.kr` 서브도메인 발급 → 즉시 `ACTIVE`
2. 사용자가 DNS 레코드 추가 (예: A 레코드 `myblog → 203.0.113.10`)
3. 내부 처리:
   ```
   DnsRecordService.addRecord()
     → dns_records INSERT
     → SYNC_DNS 잡 enqueue
   RegistrarJobWorker (5초마다 폴링)
     → resolveZoneName() = "domon.kr"
     → CloudflareApiClient: create/update/delete (reconciliation)
   ```
4. Cloudflare에 `myblog.domon.kr A 203.0.113.10` 생성됨 (comment: `domainon:myblog.domon.kr`)

### 지원 레코드

| 지원 | 타입 |
|------|------|
| ✅ | A, AAAA, CNAME, MX, TXT, NS |
| ❌ | SRV (Cloudflare가 `data` 객체를 요구 → 현재 미지원, warning 후 skip) |

> 모든 레코드는 `proxied=false`(DNS-only, 오렌지클라우드 없음)로 생성된다. TXT/MX/NS는 원래 프록시 불가.

### 서브도메인 네임서버 위임 (사용자가 "내 네임서버"를 쓰고 싶을 때)

사용자가 `myblog.domon.kr`을 **자기 네임서버로 옮기고 싶다면** 앱의 "네임서버 변경" 기능을 쓴다. 내부 동작:

```
DomainManagementService.updateNameservers()
  → domain_nameservers 저장
  → UPDATE_NS 잡 enqueue
RegistrarJobWorker.handleUpdateNs()
  → resolveZoneName() = "domon.kr"
  → PartnerRegistrarClient.updateNameservers("domon.kr", "myblog.domon.kr", ns)
  → Cloudflare에 NS 위임 레코드 생성:
       myblog.domon.kr  NS  ns1.사용자서버
       myblog.domon.kr  NS  ns2.사용자서버
  → 기존 우리 A/MX/TXT 레코드 삭제 (이제 위임됨)
```

- 위임된 서브도메인은 우리가 더 이상 DNS를 서빙하지 않으므로, A/MX/TXT 동기화(`SYNC_DNS`)도 **스킵**된다.
- 네임서버를 다시 비우면(위임 해제) NS 레코드만 삭제되고, A/MX/TXT는 다시 동기화 대상이 된다.

---

## 7. 정상 동작 확인

### 방법 A — Cloudflare 대시보드
DNS → Records 에서 `myblog.domon.kr` 레코드가 생겼는지 확인.

### 방법 B — dig
```bash
dig +short myblog.domon.kr
# → 203.0.113.10
```
> DNS 전파/캐시 때문에 직후엔 안 보일 수 있음. `@8.8.8.8` 로 직접 조회하거나 1~5분 대기.

### 방법 C — 앱 로그
```
Cloudflare: created A myblog.domon.kr -> 203.0.113.10
DNS records synced for: myblog.domon.kr -> zone domon.kr (1 records)
```

---

## 8. 트러블슈팅

| 증상 | 원인 / 해결 |
|------|-------------|
| 앱 기동 실패 | `CLOUDFLARE_API_TOKEN` 미설정 → 토큰 설정 |
| `Cloudflare zone not found` | 도메인이 Cloudflare에 추가 안 됨 / NS 변경 안 됨 / 토큰에 `Zone:Read` 없음 |
| `Cloudflare API error 403` | 토큰에 `DNS:Edit` 권한 없음, 또는 zone scope 오타 |
| `Cloudflare API error 400` | 레코드 형식 오류 (TTL 범위, SRV 등 지원 안 되는 타입) |
| 레코드가 안 생김 | 로그에서 `No DNS zone resolved` 확인 → 플랫폼 도메인이 없거나 연결 안 됨 |
| 동기화가 스킵됨 | `registrar_jobs` 큐가 `DEAD` 상태인지 확인 (재시도 5회 초과) |

---

## 9. 설정 레퍼런스

| 환경변수 | 기본값 | 설명 |
|----------|--------|------|
| `REGISTRAR_MODE` | `stub` | `partner` 로 설정 시 Cloudflare 연동 |
| `CLOUDFLARE_API_TOKEN` | (빈값) | Zone:DNS Edit 권한 API 토큰 |
| `CLOUDFLARE_BASE_URL` | `https://api.cloudflare.com/client/v4` | API 엔드포인트 |
| `PAYMENT_MODE` | `stub` | 결제 게이트웨이 모드 (레지스트라와 독립) |

---

## 10. 동작 원리 (짧게)

1. **DB가 source of truth** — 레코드는 항상 `dns_records`에 저장, Cloudflare 반영은 "동기화"로 취급
2. **비동기 + 재시도** — 5초 폴링, 실패 시 지수 백오프(2s→4s→8s→16s) → 5회 초과 시 DEAD
3. **reconciliation** — create(추가) / update(변경) / delete(삭제) 3-way 동기화
4. **포트-어댑터** — `RegistrarClient` 포트로 `stub` ↔ `partner(Cloudflare)` 교체

> 관련 상세 설계: [`DOMAIN_FLOW.md` §8](DOMAIN_FLOW.md) · [`ARCHITECTURE.md`](ARCHITECTURE.md)
