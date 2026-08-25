-- V25: 관리자 페이지에서 설정하는 앱 설정(SMTP/외부 API 키) key-value 저장.

CREATE TABLE app_settings (
    key         VARCHAR(100) PRIMARY KEY,
    value       TEXT,
    description VARCHAR(255),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed: 기본값(빈 값은 해당 기능 비활성화/미설정).
INSERT INTO app_settings (key, value, description) VALUES
('smtp.host',            'smtp.naver.com', 'SMTP 서버 호스트'),
('smtp.port',            '587',            'SMTP 포트'),
('smtp.username',        '',               'SMTP 계정(이메일)'),
('smtp.password',        '',               'SMTP 비밀번호'),
('smtp.from',            '',               '발신자 이메일'),
('safebrowsing.api-key', '',               'Google Safe Browsing API 키 (비우면 비활성화)'),
('ctas.api-key',         '',               'KISA C-TAS API 키 (비우면 비활성화)'),
('ctas.base-url',        '',               'KISA C-TAS API 주소');
