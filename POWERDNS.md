# PowerDNS 자체 DNS 서버 연동

서브도메인 DNS 호스팅을 Cloudflare 단일 존(레코드 200개 한도)에 의존하지 않고,
**자체 운영하는 PowerDNS**로 서빙하기 위한 구성과 코드를 정리한 문서입니다.

> 기존 Cloudflare 연동(`CloudflareRegistrarClient`)은 그대로 유지되며,
> `app.registrar.mode` 값으로 Cloudflare / PowerDNS를 선택할 수 있습니다.

---

## 1. 왜 PowerDNS인가

| 구분 | 기존(Cloudflare partner) | PowerDNS |
|---|---|---|
| 레코드 한도 | 존당 200개(프리) | **제한 없음**(DB 용량이 한계) |
| API 호출 | 레코드 변경마다 외부 API 호출 | 없음(DB에서 직접 서빙) |
| 확장성 | 다수 사용자에 부적합 | 사용자 수에 선형 확장 |

구조: 서브도메인 레코드를 `rog.kr` PowerDNS 존에 저장하고, `rog.kr`의 NS를
PowerDNS로 위임하면 모든 `*.rog.kr` 질의를 자체 서버가 응답합니다.

---

## 2. 아키텍처

`RegistrarClient` 인터페이스(`domain-core`)에 대해 구현체를 2개 제공하고,
`app.registrar.mode`로 선택합니다.

```
modules/domain-registrar/src/main/java/com/domainreg/registrar/
├── cloudflare/ CloudflareApiClient, CloudflareRegistrarClient (mode=cloudflare)
├── powerdns/  PowerDnsApiClient, PowerDnsRegistrarClient    (mode=powerdns)
└── stub/      StubRegistrarClient                           (mode=stub)
```

`PowerDnsRegistrarClient`는 PowerDNS REST API(`/api/v1`, 로컬 8081)를 통해
존의 rrset을 동기화합니다. `RegistrarJobWorker`의 `SYNC_DNS` 잡이 기존과 동일하게
`registrarClient.syncDnsRecords(...)`를 호출하므로, 나머지 플랫폼 코드는 그대로입니다.

---

## 3. 서버 설치/구성 (1회)

```bash
# 1) 설치
sudo apt-get update
sudo apt-get install -y pdns-server pdns-backend-pgsql

# 2) 전용 DB + 유저
sudo -u postgres psql -c "CREATE ROLE pdns WITH LOGIN PASSWORD '<PDNS_PASSWORD>';"
sudo -u postgres psql -c "CREATE DATABASE powerdns OWNER pdns;"
PGPASSWORD='<PDNS_PASSWORD>' psql -h 127.0.0.1 -U pdns -d powerdns \
  -f /usr/share/pdns-backend-pgsql/schema/schema.pgsql.sql

# 3) 설정 (bind 백엔드 비활성화 + gpgsql 백엔드 활성화)
sudo mv /etc/powerdns/pdns.d/bind.conf /etc/powerdns/pdns.d/bind.conf.disabled
sudo tee /etc/powerdns/pdns.d/gpgsql.conf >/dev/null <<'EOF'
launch=gpgsql
gpgsql-host=127.0.0.1
gpgsql-port=5432
gpgsql-dbname=powerdns
gpgsql-user=pdns
gpgsql-password=<PDNS_PASSWORD>
gpgsql-dnssec=yes

local-address=10.0.0.159      # 서버 사설 IP (공인 IP는 NAT로 이 IP에 도달)
local-port=53

daemon=yes
guardian=yes
setuid=pdns
setgid=pdns

api=yes
api-key=<생성한 api-key>
webserver=yes
webserver-address=127.0.0.1
webserver-port=8081
EOF

# 4) 시작
sudo systemctl enable --now pdns
```

---

## 4. 존(zone) 생성

PowerDNS는 `rog.kr` 존 하나에 모든 서브도메인 레코드를 저장합니다.
(서브도메인마다 존을 만들 필요 없음 — 그래서 레코드 한도가 없음)

```sql
-- domains
INSERT INTO domains (name, type) VALUES ('rog.kr', 'NATIVE');

-- SOA + NS + 글루(네임서버) A 레코드
INSERT INTO records (domain_id, name, type, content, ttl) VALUES
  (<zone_id>, 'rog.kr',    'SOA', 'ns1.rog.kr. hostmaster.rog.kr. 2024082401 10800 3600 604800 3600', 3600),
  (<zone_id>, 'rog.kr',    'NS',  'ns1.rog.kr.', 3600),
  (<zone_id>, 'rog.kr',    'NS',  'ns2.rog.kr.', 3600),
  (<zone_id>, 'ns1.rog.kr','A',   '217.142.144.114', 3600),
  (<zone_id>, 'ns2.rog.kr','A',   '217.142.144.114', 3600);
```

---

## 5. 앱 실행 설정

`application.yml`:

```yaml
app:
  registrar:
    mode: ${REGISTRAR_MODE:stub}          # cloudflare | powerdns | stub
  powerdns:
    base-url: ${POWERDNS_BASE_URL:http://127.0.0.1:8081}
    api-key: ${POWERDNS_API_KEY:}
```

### PowerDNS 모드로 실행
```bash
REGISTRAR_MODE=powerdns \
POWERDNS_API_KEY=<api-key> \
java -jar services/api/target/api-0.1.0-SNAPSHOT.jar
```

### Cloudflare 모드로 실행 (기존)
```bash
REGISTRAR_MODE=cloudflare \
CLOUDFLARE_API_TOKEN=cfut_... \
java -jar services/api/target/api-0.1.0-SNAPSHOT.jar
```

`.vscode/launch.json`의 `env`에도 `REGISTRAR_MODE`/`POWERDNS_API_KEY`를
원하는 모드에 맞게 넣으면 VSCode 디버그에서도 동일하게 동작합니다.

---

## 6. NS 전환 (공개 서빙 활성화)

PowerDNS가 공개 DNS로 응답하려면 **rog.kr의 NS를 호스팅KR에서 PowerDNS로 변경**해야 합니다.

1. 호스팅KR 도메인 관리 → rog.kr 네임서버 변경
2. **호스트(글루) 레코드** 2개 등록:
   - `ns1.rog.kr` → `217.142.144.114`
   - `ns2.rog.kr` → `217.142.144.114`
3. NS를 `ns1.rog.kr`, `ns2.rog.kr` 로 설정

> Oracle Cloud 인스턴스라면 **보안그룹/네트워크 보안그룹에서 53/UDP, 53/TCP 인바운드를 열어야** 합니다.
> (`ufw`는 inactive 상태로 확인됨)

전환 완료 후 확인:
```bash
dig @8.8.8.8 test.aiworks.rog.kr A
```

---

## 7. 동작 흐름

1. 사용자가 DNS 레코드 추가 → `DnsRecordService`가 DB에 저장하고 `SYNC_DNS` 잡 생성.
2. `RegistrarJobWorker`가 잡을 처리 → `registrarClient.syncDnsRecords(zone, domain, records)`.
3. `PowerDnsRegistrarClient`가 PowerDNS API로 rog.kr 존의 rrset을 **DELETE(삭제) + REPLACE(생성/갱신)** 로 동기화.
4. PowerDNS가 DB에서 직접 서빙 → `*.rog.kr` 질의 응답.

지원 레코드 타입: `A`, `AAAA`, `CNAME`, `MX`, `TXT`, `NS`
(CNAME/NS/MX는 FQDN 뒤에 `.`을 붙여 절대 이름으로 전송)

---

## 8. 참고: 빈 레코드(empty non-terminal)

PowerDNS DB에 `type`/`content`가 비어 있는 `blog.rog.kr` 같은 행이 보이는 것은
**정상**입니다. 하위 레코드(`aaa.blog.rog.kr`)만 있고 상위 이름 자체에는 레코드가 없을 때
PowerDNS가 자동으로 관리하는 "empty non-terminal"이며, DNS 응답에는 포함되지 않습니다.
