package com.domainreg.controller;

import com.domainreg.core.entity.Comment;
import com.domainreg.core.entity.Post;
import com.domainreg.dto.CreateCommentRequest;
import com.domainreg.dto.CreatePostRequest;
import com.domainreg.dto.UpdatePostRequest;
import com.domainreg.security.UserPrincipal;
import com.domainreg.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/board")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /** 목록 — 공개(제목만). */
    @GetMapping("/posts")
    public ResponseEntity<List<Post>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(boardService.getPosts(principal));
    }

    /** 상세 — 로그인 필수. */
    @GetMapping("/posts/{id}")
    public ResponseEntity<Post> detail(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(boardService.getPost(id, principal));
    }

    /** 댓글 목록 — 로그인 필수(상세에서 사용). */
    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<List<Comment>> comments(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.getComments(id));
    }

    /** 글 작성 — 로그인 필수. */
    @PostMapping("/posts")
    public ResponseEntity<Post> create(@Valid @RequestBody CreatePostRequest req,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.createPost(req, principal));
    }

    /** 비밀글 비밀번호 확인 — 로그인 필수. */
    @PostMapping("/posts/{id}/verify")
    public ResponseEntity<Post> verify(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(boardService.verifyPassword(id, body.get("password")));
    }

    /** 댓글 작성 — 로그인 필수. */
    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<Comment> createComment(@PathVariable Long id,
                                                 @Valid @RequestBody CreateCommentRequest req,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(boardService.createComment(id, req, principal));
    }

    /** 글 수정 — 작성자/관리자. */
    @PutMapping("/posts/{id}")
    public ResponseEntity<Post> updatePost(@PathVariable Long id,
                                           @Valid @RequestBody UpdatePostRequest req,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(boardService.updatePost(id, req, principal));
    }

    /** 댓글 수정 — 작성자/관리자. */
    @PutMapping("/comments/{id}")
    public ResponseEntity<Comment> updateComment(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(boardService.updateComment(id, body.get("content"), principal));
    }

    /** 글 삭제 — 작성자/관리자. */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Map<String, String>> deletePost(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        boardService.deletePost(id, principal);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    /** 댓글 삭제 — 작성자/관리자. */
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Map<String, String>> deleteComment(@PathVariable Long id,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        boardService.deleteComment(id, principal);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }
}
