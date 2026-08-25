package com.domainreg.core.port;

import com.domainreg.core.entity.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    List<Post> findAll(boolean includeHidden);
    List<Post> findAllAdmin();
    Optional<Post> findById(Long id);
    Post save(Post post);
    void softDelete(Long id);
    void restore(Long id);
    void deleteById(Long id);
    void incrementViewCount(Long id);
}
