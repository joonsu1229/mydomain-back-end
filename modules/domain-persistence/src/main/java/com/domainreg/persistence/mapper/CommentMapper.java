package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CommentMapper {
    List<Comment> findByPostId(@Param("postId") Long postId);
    Optional<Comment> findById(@Param("id") Long id);
    void insert(Comment comment);
    void update(Comment comment);
    void delete(@Param("id") Long id);
}
