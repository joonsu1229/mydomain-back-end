-- V22: Q&A 게시판 — 글(posts) + 댓글/대댓글(comments).
-- 읽기·쓰기 모두 로그인 필요. 목록(제목)만 비로그인 공개.

CREATE TABLE posts (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    author_name   VARCHAR(50) NOT NULL,                -- 회원명 또는 '익명'
    password_hash VARCHAR(100),                        -- 비밀글 비밀번호(BCrypt, is_secret일 때만)
    title         VARCHAR(200) NOT NULL,
    content       TEXT        NOT NULL,                -- HTML (에디터)
    is_notice     BOOLEAN     NOT NULL DEFAULT FALSE,  -- 공지사항(관리자만)
    is_secret     BOOLEAN     NOT NULL DEFAULT FALSE,  -- 비밀글(비밀번호로 열람)
    is_hidden     BOOLEAN     NOT NULL DEFAULT FALSE,  -- 비공개(관리자 처리)
    view_count    INT         NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_user    ON posts(user_id);
CREATE INDEX idx_posts_created ON posts(created_at);

CREATE TABLE comments (
    id          BIGSERIAL PRIMARY KEY,
    post_id     BIGINT      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    parent_id   BIGINT      REFERENCES comments(id) ON DELETE CASCADE, -- NULL=최상위 댓글, 값=대댓글
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    author_name VARCHAR(50) NOT NULL,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_post   ON comments(post_id);
CREATE INDEX idx_comments_parent ON comments(parent_id);
