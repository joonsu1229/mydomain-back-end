package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PostMapper {
    List<Post> findAll(@Param("includeHidden") boolean includeHidden);
    Optional<Post> findById(@Param("id") Long id);
    void insert(Post post);
    void update(Post post);
    void delete(@Param("id") Long id);
    void incrementViewCount(@Param("id") Long id);
}
