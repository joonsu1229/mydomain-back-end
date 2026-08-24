package com.domainreg.core.port;

import com.domainreg.core.entity.Terms;

import java.util.List;
import java.util.Optional;

public interface TermsRepository {
    List<Terms> findAll();
    List<Terms> findCurrentAll();
    Optional<Terms> findById(Long id);
    Integer findMaxVersion(String type);
    Terms insert(Terms terms);
    void updateContent(Long id, String title, String content);
    void clearCurrent(String type);
    void publish(Long id);
    void deleteById(Long id);
}
