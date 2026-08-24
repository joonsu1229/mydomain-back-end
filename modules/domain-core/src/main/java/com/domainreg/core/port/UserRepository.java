package com.domainreg.core.port;

import com.domainreg.core.entity.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByLoginId(String loginId);
    User save(User user);
    boolean existsByEmail(String email);
    boolean existsByLoginId(String loginId);

    void recordLoginFailure(Long id);
    void lockAccount(Long id, java.time.Instant lockedUntil);
    void recordLoginSuccess(Long id, String ip);
    void unlockAccount(Long id);
}
