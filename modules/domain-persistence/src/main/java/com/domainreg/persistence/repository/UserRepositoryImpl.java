package com.domainreg.persistence.repository;

import com.domainreg.core.entity.User;
import com.domainreg.core.port.UserRepository;
import com.domainreg.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper mapper;

    public UserRepositoryImpl(UserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return mapper.findByEmail(email);
    }

    @Override
    public Optional<User> findByLoginId(String loginId) {
        return mapper.findByLoginId(loginId);
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            mapper.insert(user);
        } else {
            mapper.update(user);
        }
        return user;
    }

    @Override
    public boolean existsByEmail(String email) {
        return mapper.existsByEmail(email);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return mapper.existsByLoginId(loginId);
    }

    @Override
    public void recordLoginFailure(Long id) {
        mapper.recordLoginFailure(id);
    }

    @Override
    public void lockAccount(Long id, java.time.Instant lockedUntil) {
        mapper.lockAccount(id, lockedUntil);
    }

    @Override
    public void recordLoginSuccess(Long id, String ip) {
        mapper.recordLoginSuccess(id, ip);
    }

    @Override
    public void unlockAccount(Long id) {
        mapper.unlockAccount(id);
    }
}
