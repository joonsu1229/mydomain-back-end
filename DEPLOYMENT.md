# 도메인온 배포 가이드

프론트(Vue)와 백엔드(Spring Boot)를 **하나의 jar**로 묶어 배포하는 방법을 정리합니다.
별도 프론트 서버 없이, Spring Boot가 프론트 정적 파일과 API를 함께 서빙합니다.

> 도메인은 예시로 `mydomain.rog.kr`을 씁니다. 실제 공식 도메인으로 바꾸면 됩니다.

---

## 1. 아키텍처

```
브라우저
   │  https://mydomain.rog.kr
   ▼
NGINX (80/443, TLS)
   │  proxy_pass → localhost:5174
   ▼
Spring Boot jar (포트 5174)
   ├── /            → 프론트 index.html (static)
   ├── /assets/**   → 프론트 JS/CSS
   └── /api/**      → REST API
```

- 프론트는 `vue-tsc && vite build`로 `apps/web/dist`에 빌드.
- 그 결과물을 `services/api/src/main/resources/static/`에 복사하면 Spring Boot가 `/`에서 자동 서빙.
- 라우터가 **hash history**(`/#/dashboard`)라서 서버측 SPA fallback 설정이 필요 없음.

---

## 2. 빌드 (프론트 → 백엔드 통합)

`deploy.sh` 한 방으로 됩니다:

```bash
chmod +x deploy.sh
./deploy.sh
```

내부 동작:
1. `apps/web` → `npm install && npm run build`
2. `dist/*` → `services/api/src/main/resources/static/`
3. `mvn -o -q -DskipTests -pl services/api -am package`
4. `systemctl restart domainon`

수동으로 하려면:
```bash
cd apps/web && npm run build
rm -rf services/api/src/main/resources/static
mkdir -p services/api/src/main/resources/static
cp -r apps/web/dist/. services/api/src/main/resources/static/
mvn -o -q -DskipTests -pl services/api -am package
```

---

## 3. systemd 서비스

`/etc/systemd/system/domainon.service`:

```ini
[Unit]
Description=DomainOn API + Web
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/domain-platform/workspace/web-domain-reg
Environment=DB_HOST=217.142.144.114
Environment=DB_USER=postgreuser
Environment=DB_PASS=<DB_PASSWORD>
Environment=DB_SCHEMA=domaindb
Environment=REDIS_HOST=localhost
Environment=REDIS_PORT=6379
Environment=JWT_SECRET=your-256-bit-key-minimum-32-chars!
Environment=REGISTRAR_MODE=powerdns
Environment=POWERDNS_API_KEY=<POWERDNS_API_KEY>
ExecStart=/usr/bin/java -jar services/api/target/api-0.1.0-SNAPSHOT.jar
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=multi-user.target
```

등록/시작:
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now domainon
sudo systemctl status domainon
```

> `REGISTRAR_MODE`는 `powerdns`(자체 DNS) 또는 `cloudflare` 선택.

---

## 4. NGINX 리버스 프록시

`/etc/nginx/sites-available/mydomain.rog.kr`:

```nginx
server {
    listen 80;
    server_name mydomain.rog.kr;

    # 백엔드(프론트+API)로 전달
    location / {
        proxy_pass http://127.0.0.1:5174;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

적용:
```bash
sudo ln -s /etc/nginx/sites-available/mydomain.rog.kr /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

HTTPS는 `certbot --nginx -d mydomain.rog.kr` (Let's Encrypt)로 추가.

> 주의: 이 서버는 kasm이 443을 쓰고 있음. NGINX로 443을 쓰려면 kasm 포트를 8443 등으로 옮기거나,
> HTTP(80)만 쓰거나, kasm은 Cloudflare Tunnel(또는 다른 포트)로 분리해야 함.

---

## 5. Jenkins CI/CD

`Jenkinsfile`(저장소 루트에 있음)이 프론트 빌드 → static 복사 → 백엔드 패키징 → 재시작을 수행.

Jenkins 쪽 설정:
1. Jenkins 관리 → Tools에서 **Maven** 등록.
2. 새 Item → **Pipeline** → Pipeline from SCM → Git(GitHub 저장소 URL).
3. Jenkins 유저가 `sudo systemctl restart domainon`을 실행할 수 있도록 sudoers에 추가:
   ```
   jenkins ALL=(ALL) NOPASSWD: /bin/systemctl restart domainon
   ```

---

## 6. GitHub 올리기

### 6-1. 저장소 초기화 & 푸시
```bash
cd /home/ubuntu/domain-platform/workspace/web-domain-reg
git init
git add .
git commit -m "Initial commit"
# GitHub에서 저장소 생성 후
git remote add origin https://github.com/<계정>/<repo>.git
git branch -M main
git push -u origin main
```

### 6-2. ⚠️ 시크릿 처리 (필수)
현재 코드에 **시크릿이 하드코딩**되어 있으니 GitHub에 올리기 전에 반드시 제거하세요:

| 파일 | 노출되는 값 |
|---|---|
| `services/api/src/main/resources/application.yml` | DB 비밀번호, SMTP 비밀번호(JWT 시크릿 기본값) |
| `.vscode/launch.json` | DB 비밀번호, Cloudflare 토큰, PowerDNS API 키 |

권장:
- `application.yml`의 하드코딩 기본값을 제거하고 **환경변수/외부설정**으로만 주입.
  (이미 `${DB_PASS:...}` 형태이므로 `:...` 기본값만 지우면 됨)
- `.vscode/`는 `.gitignore`에 이미 포함되어 커밋에서 제외됨.
- GitHub는 **private 저장소**로 만들고, 시크릿은 Jenkins Credentials / 서버 환경변수로 관리.

---

## 7. 배포 흐름 요약

```
코드 수정 → GitHub push → Jenkins가 자동 빌드(deploy.sh) → jar 교체 → systemctl restart
```
- 로컬 즉시 반영: `./deploy.sh`
- CI 자동 반영: GitHub push → Jenkins Pipeline
