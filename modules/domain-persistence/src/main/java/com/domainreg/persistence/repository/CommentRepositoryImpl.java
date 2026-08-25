package com.domainreg.persistence.repository;

import com.domainreg.core.entity.Comment;
import com.domainreg.core.port.CommentRepository;
import com.domainreg.persistence.mapper.CommentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CommentRepositoryImpl implements CommentRepository {

    private final CommentMapper mapper;

    public CommentRepositoryImpl(CommentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Comment> findByPostId(Long postId) {
        return mapper.findByPostId(postId);
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public Comment save(Comment comment) {
        if (comment.getId() == null) {
            mapper.insert(comment);
        } else {
            mapper.update(comment);
        }
        return comment;
    }

    @Override
    public void deleteById(Long id) {
        mapper.delete(id);
    }
}
