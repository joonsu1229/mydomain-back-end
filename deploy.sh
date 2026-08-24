#!/usr/bin/env bash
# 도메인온 배포 스크립트
# 프론트(Vue) 빌드 → 백엔드 static 복사 → jar 패키징 → 서비스 재시작
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEB="$ROOT/apps/web"
STATIC="$ROOT/services/api/src/main/resources/static"
JAR="$ROOT/services/api/target/api-0.1.0-SNAPSHOT.jar"
SERVICE="domainon"   # systemd 서비스명

echo "==> [1/4] 프론트 빌드"
cd "$WEB"
npm install
npm run build

echo "==> [2/4] dist → 백엔드 static 복사"
rm -rf "$STATIC"
mkdir -p "$STATIC"
cp -r "$WEB/dist/." "$STATIC/"

echo "==> [3/4] 백엔드 jar 패키징"
cd "$ROOT"
mvn -o -q -DskipTests -pl services/api -am package

echo "==> [4/4] 서비스 재시작"
if systemctl is-enabled "$SERVICE" >/dev/null 2>&1; then
  sudo systemctl restart "$SERVICE"
  echo "완료: $SERVICE 재시작됨"
else
  echo "완료: $JAR 빌드됨 (systemd 미등록 — 수동 실행 필요)"
fi
