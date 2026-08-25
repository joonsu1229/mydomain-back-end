package com.domainreg.core.entity;

import java.time.Instant;

public class Comment {

    private Long id;
    private Long postId;
    private Long parentId;   // NULL = 최상위 댓글, 값 = 대댓글(1단계만)
    private Long userId;
    private String authorName;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    public static Comment create(Long postId, Long parentId, Long userId, String authorName, String content) {
        Comment c = new Comment();
        c.postId = postId;
        c.parentId = parentId;
        c.userId = userId;
        c.authorName = authorName;
        c.content = content;
        return c;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
