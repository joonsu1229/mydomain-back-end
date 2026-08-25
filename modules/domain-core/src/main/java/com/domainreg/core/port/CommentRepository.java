package com.domainreg.core.port;

import com.domainreg.core.entity.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    List<Comment> findByPostId(Long postId);
    Optional<Comment> findById(Long id);
    Comment save(Comment comment);
    void deleteById(Long id);
}
