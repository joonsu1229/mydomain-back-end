package com.domainreg.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class Post {

    private Long id;
    private Long userId;
    private String authorName;
    private String loginId;        // 작성자 로그인 아이디(익명 글은 NULL) — 목록 검색용
    private String passwordHash;   // 비밀글 비밀번호(BCrypt, is_secret일 때만) — 응답에서 제외
    private String title;
    private String content;        // HTML (에디터)
    private boolean isNotice;
    private boolean isSecret;
    private boolean isHidden;
    private String useYn = "Y";   // 사용 여부(Y=활성, N=삭제) — 회원 삭제는 N 처리
    private int viewCount;
    private int commentCount;      // 목록 집계
    private Instant createdAt;
    private Instant updatedAt;

    public static Post create(Long userId, String authorName, String title, String content,
                              boolean isNotice, boolean isSecret, String passwordHash) {
        Post p = new Post();
        p.userId = userId;
        p.authorName = authorName;
        p.title = title;
        p.content = content;
        p.isNotice = isNotice;
        p.isSecret = isSecret;
        p.passwordHash = passwordHash;
        return p;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    @JsonIgnore
    public String getPasswordHash() { return passwordHash; }
    @JsonIgnore
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @JsonProperty("isNotice")
    public boolean isNotice() { return isNotice; }
    @JsonProperty("isNotice")
    public void setNotice(boolean notice) { this.isNotice = notice; }

    @JsonProperty("isSecret")
    public boolean isSecret() { return isSecret; }
    @JsonProperty("isSecret")
    public void setSecret(boolean secret) { this.isSecret = secret; }

    @JsonProperty("isHidden")
    public boolean isHidden() { return isHidden; }
    @JsonProperty("isHidden")
    public void setHidden(boolean hidden) { this.isHidden = hidden; }

    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
