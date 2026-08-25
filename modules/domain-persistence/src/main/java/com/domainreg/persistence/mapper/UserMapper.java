package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findById(@Param("id") Long id);
    Optional<User> findByEmail(@Param("email") String email);
    Optional<User> findByLoginId(@Param("loginId") String loginId);
    void insert(User user);
    void update(User user);
    boolean existsByEmail(@Param("email") String email);
    boolean existsByLoginId(@Param("loginId") String loginId);
    Optional<User> findByVerificationToken(@Param("token") String token);
    void verifyEmail(@Param("id") Long id);
    void updateVerificationToken(@Param("id") Long id, @Param("token") String token);
    long countAll();
    List<User> findAll();

    void recordLoginFailure(@Param("id") Long id);
    void lockAccount(@Param("id") Long id, @Param("lockedUntil") java.time.Instant lockedUntil);
    void recordLoginSuccess(@Param("id") Long id, @Param("ip") String ip);
    void unlockAccount(@Param("id") Long id);
    void updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);
    void updateEmail(@Param("id") Long id, @Param("email") String email);
    void updatePermissions(@Param("id") Long id, @Param("nsEnabled") boolean nsEnabled, @Param("privacyEnabled") boolean privacyEnabled);
    void deletePaymentsByUserId(@Param("id") Long id);
    void deleteOrdersByUserId(@Param("id") Long id);
    void deleteById(@Param("id") Long id);
}
