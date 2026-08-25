package com.domainreg.controller;

import com.domainreg.core.entity.Post;
import com.domainreg.service.BoardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/board")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBoardController {

    private final BoardService boardService;

    public AdminBoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /** 전체 게시글 목록(삭제 포함) — 관리자 전용. */
    @GetMapping("/posts")
    public ResponseEntity<List<Post>> posts() {
        return ResponseEntity.ok(boardService.getAllPostsForAdmin());
    }

    /** 삭제된 글 복원. */
    @PutMapping("/posts/{id}/restore")
    public ResponseEntity<Post> restore(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.restorePost(id));
    }

    /** 영구삭제. */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Map<String, String>> permanentDelete(@PathVariable Long id) {
        boardService.permanentDeletePost(id);
        return ResponseEntity.ok(Map.of("message", "영구 삭제되었습니다."));
    }

    /** 비공개 처리 토글. */
    @PutMapping("/posts/{id}/hide")
    public ResponseEntity<Post> hide(@PathVariable Long id, @RequestBody Map<String, String> body) {
        boolean hidden = Boolean.parseBoolean(body.getOrDefault("hidden", "true"));
        return ResponseEntity.ok(boardService.setHidden(id, hidden));
    }

    /** 공지사항 토글. */
    @PutMapping("/posts/{id}/notice")
    public ResponseEntity<Post> notice(@PathVariable Long id, @RequestBody Map<String, String> body) {
        boolean notice = Boolean.parseBoolean(body.getOrDefault("notice", "true"));
        return ResponseEntity.ok(boardService.setNotice(id, notice));
    }
}
