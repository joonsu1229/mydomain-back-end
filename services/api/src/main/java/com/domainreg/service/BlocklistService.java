package com.domainreg.service;

import com.domainreg.core.entity.BlocklistKeyword;
import com.domainreg.core.port.BlocklistKeywordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 블랙리스트 키워드 관리(관리자 CRUD) + 검증용 활성 키워드 조회.
 */
@Service
public class BlocklistService {

    private static final Set<String> CATEGORIES = Set.of(
        BlocklistKeyword.CATEGORY_IMPERSONATION,
        BlocklistKeyword.CATEGORY_RESERVED);

    private final BlocklistKeywordRepository repository;

    public BlocklistService(BlocklistKeywordRepository repository) {
        this.repository = repository;
    }

    public List<BlocklistKeyword> findAll() {
        return repository.findAll();
    }

    /** 검증 시 사용하는 활성 키워드 목록. */
    public List<BlocklistKeyword> findEnabled() {
        return repository.findAllEnabled();
    }

    public BlocklistKeyword add(String keyword, String category, String note) {
        String kw = normalize(keyword);
        if (kw.isEmpty()) {
            throw new BlocklistException("EMPTY_KEYWORD", "키워드를 입력해주세요.");
        }
        validateCategory(category);
        if (repository.existsByKeyword(kw)) {
            throw new BlocklistException("DUPLICATE_KEYWORD", "이미 등록된 키워드입니다.");
        }
        return repository.save(BlocklistKeyword.create(kw, category, note));
    }

    public BlocklistKeyword update(Long id, String keyword, String category, String note, Boolean enabled) {
        BlocklistKeyword k = repository.findById(id)
            .orElseThrow(() -> new BlocklistException("NOT_FOUND", "키워드를 찾을 수 없습니다."));
        String kw = normalize(keyword);
        if (kw.isEmpty()) {
            throw new BlocklistException("EMPTY_KEYWORD", "키워드를 입력해주세요.");
        }
        validateCategory(category);
        k.setKeyword(kw);
        k.setCategory(category);
        k.setNote(note);
        if (enabled != null) {
            k.setEnabled(enabled);
        }
        return repository.save(k);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void validateCategory(String category) {
        if (category == null || !CATEGORIES.contains(category)) {
            throw new BlocklistException("INVALID_CATEGORY", "카테고리는 IMPERSONATION 또는 RESERVED 여야 합니다.");
        }
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }

    public static class BlocklistException extends RuntimeException {
        private final String code;
        public BlocklistException(String code, String message) { super(message); this.code = code; }
        public String getCode() { return code; }
    }
}
