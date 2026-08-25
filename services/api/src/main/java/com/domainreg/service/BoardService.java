package com.domainreg.service;

import com.domainreg.core.entity.Comment;
import com.domainreg.core.entity.Post;
import com.domainreg.core.entity.User;
import com.domainreg.core.port.CommentRepository;
import com.domainreg.core.port.PostRepository;
import com.domainreg.core.port.UserRepository;
import com.domainreg.dto.CreateCommentRequest;
import com.domainreg.dto.CreatePostRequest;
import com.domainreg.dto.UpdatePostRequest;
import com.domainreg.security.UserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BoardService(PostRepository postRepository,
                        CommentRepository commentRepository,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 목록(제목 공개) — 비공개 글은 관리자만 보임. */
    public List<Post> getPosts(UserPrincipal principal) {
        return postRepository.findAll(isAdmin(principal));
    }

    /** 관리자: 전체 게시글 목록(삭제 포함, 본문 포함). */
    public List<Post> getAllPostsForAdmin() {
        return postRepository.findAllAdmin();
    }

    /** 상세(로그인 필수) — 조회수 증가 + 비밀글/비공개 처리. */
    @Transactional
    public Post getPost(Long id, UserPrincipal principal) {
        Post p = requireActivePost(id);
        if (p.isHidden() && !isAdmin(principal)) {
            throw new BoardException("NOT_FOUND", "게시글을 찾을 수 없습니다.");
        }
        boolean owner = principal != null && p.getUserId().equals(principal.getUserId());
        if (p.isSecret() && !owner && !isAdmin(principal)) {
            p.setContent(null);
        }
        postRepository.incrementViewCount(id);
        return p;
    }

    /** 글 작성(로그인 필수). */
    @Transactional
    public Post createPost(CreatePostRequest req, UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new BoardException("NOT_FOUND", "사용자를 찾을 수 없습니다."));
        boolean notice = Boolean.TRUE.equals(req.isNotice()) && isAdmin(principal);
        boolean secret = Boolean.TRUE.equals(req.isSecret());
        String authorName = Boolean.TRUE.equals(req.anonymous()) ? "익명" : user.getName();
        String passwordHash = null;
        if (secret) {
            if (req.password() == null || req.password().isBlank()) {
                throw new BoardException("PASSWORD_REQUIRED", "비밀글에는 비밀번호가 필요합니다.");
            }
            passwordHash = passwordEncoder.encode(req.password());
        }
        Post post = Post.create(principal.getUserId(), authorName, req.title().trim(),
            req.content(), notice, secret, passwordHash);
        return postRepository.save(post);
    }

    /** 비밀글 비밀번호 확인(로그인 필수) — 맞으면 본문 반환. */
    public Post verifyPassword(Long id, String password) {
        Post p = requireActivePost(id);
        if (!p.isSecret()) {
            return p;
        }
        if (p.getPasswordHash() == null || password == null
                || !passwordEncoder.matches(password, p.getPasswordHash())) {
            throw new BoardException("INVALID_PASSWORD", "비밀번호가 올바르지 않습니다.");
        }
        return p;
    }

    /** 글 수정(작성자/관리자). */
    @Transactional
    public Post updatePost(Long id, UpdatePostRequest req, UserPrincipal principal) {
        Post p = requireActivePost(id);
        requirePostOwnerOrAdmin(p, principal);
        p.setTitle(req.title().trim());
        p.setContent(req.content());
        boolean secret = Boolean.TRUE.equals(req.isSecret());
        p.setSecret(secret);
        if (secret) {
            if (req.password() != null && !req.password().isBlank()) {
                p.setPasswordHash(passwordEncoder.encode(req.password()));
            } else if (p.getPasswordHash() == null) {
                throw new BoardException("PASSWORD_REQUIRED", "비밀글에는 비밀번호가 필요합니다.");
            }
        } else {
            p.setPasswordHash(null);
        }
        return postRepository.save(p);
    }

    /** 댓글 목록(로그인 필수 — 상세에서만 사용). */
    public List<Comment> getComments(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    /** 댓글 작성(로그인 필수, 1단계 대댓글). */
    @Transactional
    public Comment createComment(Long postId, CreateCommentRequest req, UserPrincipal principal) {
        Post p = requireActivePost(postId);
        if (p.isHidden() && !isAdmin(principal)) {
            throw new BoardException("NOT_FOUND", "게시글을 찾을 수 없습니다.");
        }
        Long parentId = req.parentId();
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new BoardException("INVALID_PARENT", "원 댓글을 찾을 수 없습니다."));
            if (parent.getParentId() != null || !parent.getPostId().equals(postId)) {
                throw new BoardException("INVALID_PARENT", "대댓글에는 답글을 달 수 없습니다.");
            }
        }
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new BoardException("NOT_FOUND", "사용자를 찾을 수 없습니다."));
        Comment c = Comment.create(postId, parentId, principal.getUserId(), user.getName(), req.content());
        return commentRepository.save(c);
    }

    /** 댓글 수정(작성자/관리자). */
    @Transactional
    public Comment updateComment(Long id, String content, UserPrincipal principal) {
        Comment c = requireComment(id);
        requireCommentOwnerOrAdmin(c, principal);
        c.setContent(content);
        return commentRepository.save(c);
    }

    /** 글 삭제 — 누구든(회원/관리자) 소프트 삭제(use_yn=N). 영구삭제는 관리자 전용 별도 메서드. */
    @Transactional
    public void deletePost(Long id, UserPrincipal principal) {
        Post p = requirePost(id);
        requirePostOwnerOrAdmin(p, principal);
        postRepository.softDelete(id);
    }

    /** 관리자: 영구삭제(댓글 cascade). */
    @Transactional
    public void permanentDeletePost(Long id) {
        requirePost(id);
        postRepository.deleteById(id);
    }

    /** 댓글 삭제(작성자/관리자) — 대댓글은 cascade. */
    @Transactional
    public void deleteComment(Long id, UserPrincipal principal) {
        Comment c = requireComment(id);
        requireCommentOwnerOrAdmin(c, principal);
        commentRepository.deleteById(id);
    }

    /** 관리자: 비공개 처리 토글. */
    @Transactional
    public Post setHidden(Long id, boolean hidden) {
        Post p = requireActivePost(id);
        p.setHidden(hidden);
        return postRepository.save(p);
    }

    /** 관리자: 공지사항 토글. */
    @Transactional
    public Post setNotice(Long id, boolean notice) {
        Post p = requireActivePost(id);
        p.setNotice(notice);
        return postRepository.save(p);
    }

    /** 관리자: 삭제된 글 복원. */
    @Transactional
    public Post restorePost(Long id) {
        Post p = requirePost(id);
        postRepository.restore(id);
        p.setUseYn("Y");
        return p;
    }

    private Post requirePost(Long id) {
        return postRepository.findById(id)
            .orElseThrow(() -> new BoardException("NOT_FOUND", "게시글을 찾을 수 없습니다."));
    }

    /** 삭제되지 않은(활성) 글만 반환 — 삭제된 글은 존재하지 않는 것처럼 처리. */
    private Post requireActivePost(Long id) {
        Post p = requirePost(id);
        if (!"Y".equals(p.getUseYn())) {
            throw new BoardException("NOT_FOUND", "게시글을 찾을 수 없습니다.");
        }
        return p;
    }

    private Comment requireComment(Long id) {
        return commentRepository.findById(id)
            .orElseThrow(() -> new BoardException("NOT_FOUND", "댓글을 찾을 수 없습니다."));
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal != null && "ADMIN".equals(principal.getRole());
    }

    private void requirePostOwnerOrAdmin(Post p, UserPrincipal principal) {
        if (isAdmin(principal)) return;
        if (principal != null && p.getUserId().equals(principal.getUserId())) return;
        throw new BoardException("FORBIDDEN", "권한이 없습니다.");
    }

    private void requireCommentOwnerOrAdmin(Comment c, UserPrincipal principal) {
        if (isAdmin(principal)) return;
        if (principal != null && c.getUserId().equals(principal.getUserId())) return;
        throw new BoardException("FORBIDDEN", "권한이 없습니다.");
    }

    public static class BoardException extends RuntimeException {
        private final String code;
        public BoardException(String code, String message) { super(message); this.code = code; }
        public String getCode() { return code; }
    }
}
