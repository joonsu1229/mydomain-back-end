package com.domainreg.persistence.repository;

import com.domainreg.core.entity.Post;
import com.domainreg.core.port.PostRepository;
import com.domainreg.persistence.mapper.PostMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PostRepositoryImpl implements PostRepository {

    private final PostMapper mapper;

    public PostRepositoryImpl(PostMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Post> findAll(boolean includeHidden) {
        return mapper.findAll(includeHidden);
    }

    @Override
    public List<Post> findAllAdmin() {
        return mapper.findAllAdmin();
    }

    @Override
    public Optional<Post> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public Post save(Post post) {
        if (post.getId() == null) {
            mapper.insert(post);
        } else {
            mapper.update(post);
        }
        return post;
    }

    @Override
    public void softDelete(Long id) {
        mapper.softDelete(id);
    }

    @Override
    public void restore(Long id) {
        mapper.restore(id);
    }

    @Override
    public void deleteById(Long id) {
        mapper.delete(id);
    }

    @Override
    public void incrementViewCount(Long id) {
        mapper.incrementViewCount(id);
    }
}
