-- 게시글 소프트 삭제: 회원 삭제 시 완전삭제 대신 use_yn='N' 처리.
ALTER TABLE posts ADD COLUMN use_yn CHAR(1) NOT NULL DEFAULT 'Y';
CREATE INDEX idx_posts_use_yn ON posts(use_yn);
