-- V24: 도메인/레코드 사전 차단을 위한 블랙리스트 키워드 + 위협정보 IOC 캐시.

CREATE TABLE blocklist_keywords (
    id         BIGSERIAL PRIMARY KEY,
    keyword    VARCHAR(100) NOT NULL,
    category   VARCHAR(30)  NOT NULL DEFAULT 'IMPERSONATION', -- IMPERSONATION(사칭) | RESERVED(예약어)
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    note       VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_blocklist_keyword ON blocklist_keywords(keyword);
CREATE INDEX idx_blocklist_enabled ON blocklist_keywords(enabled);

CREATE TABLE threat_iocs (
    id         BIGSERIAL PRIMARY KEY,
    ioc_type   VARCHAR(10)  NOT NULL,                -- IP | DOMAIN
    ioc_value  VARCHAR(255) NOT NULL,
    source     VARCHAR(30)  NOT NULL DEFAULT 'CTAS', -- CTAS (향후 확장)
    first_seen TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_seen  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_threat_ioc ON threat_iocs(source, ioc_value);
CREATE INDEX idx_threat_ioc_value ON threat_iocs(ioc_value);

-- Seed: 금융/포털 사칭(IMPERSONATION) + 시스템 예약어(RESERVED).
-- 매칭은 부분 문자열(contains)로 수행되므로 정상 이름까지 차단될 수 있는 키워드는
-- 관리자 페이지에서 제거/조정할 수 있다.
INSERT INTO blocklist_keywords (keyword, category, note) VALUES
('bank',    'IMPERSONATION', '금융 사칭'),
('pay',     'IMPERSONATION', '결제 사칭'),
('login',   'IMPERSONATION', '로그인 사칭'),
('account', 'IMPERSONATION', '계정 사칭'),
('secure',  'IMPERSONATION', '보안 사칭'),
('update',  'IMPERSONATION', '업데이트 사칭'),
('naver',   'IMPERSONATION', '포털 사칭'),
('kakao',   'IMPERSONATION', '포털 사칭'),
('google',  'IMPERSONATION', '포털 사칭'),
('apple',   'IMPERSONATION', '포털 사칭'),
('support', 'IMPERSONATION', '고객센터 사칭'),
('www',     'RESERVED', '시스템 예약어'),
('api',     'RESERVED', '시스템 예약어'),
('admin',   'RESERVED', '시스템 예약어'),
('ns1',     'RESERVED', '네임서버 예약어'),
('ns2',     'RESERVED', '네임서버 예약어'),
('mail',    'RESERVED', '메일 예약어'),
('dev',     'RESERVED', '개발 예약어');
