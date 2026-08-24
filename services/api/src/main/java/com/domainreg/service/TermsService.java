package com.domainreg.service;

import com.domainreg.core.entity.Terms;
import com.domainreg.core.entity.UserTermsAgreement;
import com.domainreg.core.port.TermsRepository;
import com.domainreg.core.port.UserTermsAgreementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TermsService {

    private final TermsRepository termsRepository;
    private final UserTermsAgreementRepository agreementRepository;

    public TermsService(TermsRepository termsRepository,
                        UserTermsAgreementRepository agreementRepository) {
        this.termsRepository = termsRepository;
        this.agreementRepository = agreementRepository;
    }

    /** Public: current (published) versions of both TERMS and PRIVACY. */
    public List<Terms> getCurrentTerms() {
        return termsRepository.findCurrentAll();
    }

    /** Admin: all versions (draft + published), with agreement counts. */
    public List<Terms> listAll() {
        return termsRepository.findAll();
    }

    /** Admin: create a new draft version (version = max + 1). */
    @Transactional
    public Terms createDraft(String type, String title, String content) {
        validateType(type);
        Terms t = Terms.create(type, title, content);
        t.setVersion(termsRepository.findMaxVersion(type) + 1);
        t.setCurrent(false);
        return termsRepository.insert(t);
    }

    /** Admin: edit a draft's title/content. Published versions are immutable. */
    @Transactional
    public Terms updateDraft(Long id, String title, String content) {
        Terms t = getById(id);
        if (t.isCurrent()) {
            throw new TermsException("INVALID_STATE",
                "발행된 약관은 수정할 수 없습니다. 새 버전을 작성해주세요.");
        }
        termsRepository.updateContent(id, title, content);
        t.setTitle(title);
        t.setContent(content);
        return t;
    }

    /** Admin: publish a draft — becomes the current version for its type. */
    @Transactional
    public Terms publish(Long id) {
        Terms t = getById(id);
        if (t.isCurrent()) {
            throw new TermsException("INVALID_STATE", "이미 발행된 약관입니다.");
        }
        termsRepository.clearCurrent(t.getType());
        termsRepository.publish(id);
        t.setCurrent(true);
        t.setPublishedAt(Instant.now());
        return t;
    }

    /** Admin: delete a draft. Published versions cannot be deleted. */
    @Transactional
    public void deleteDraft(Long id) {
        Terms t = getById(id);
        if (t.isCurrent()) {
            throw new TermsException("INVALID_STATE",
                "발행된 약관은 삭제할 수 없습니다.");
        }
        termsRepository.deleteById(id);
    }

    /**
     * Record a user's agreement to the current TERMS and PRIVACY at signup.
     * Validates that each id points to the current version of the expected type.
     */
    @Transactional
    public void recordAgreements(Long userId, Long termsId, Long privacyId, String ip) {
        validateAndRecord(userId, termsId, Terms.TYPE_TERMS, ip);
        validateAndRecord(userId, privacyId, Terms.TYPE_PRIVACY, ip);
    }

    private void validateAndRecord(Long userId, Long termsId, String expectedType, String ip) {
        Terms t = termsRepository.findById(termsId)
            .orElseThrow(() -> new TermsException("INVALID_TERMS", "유효하지 않은 약관 버전입니다."));
        if (!expectedType.equals(t.getType()) || !t.isCurrent()) {
            throw new TermsException("INVALID_TERMS", "유효하지 않은 약관 버전입니다.");
        }
        agreementRepository.insert(UserTermsAgreement.create(userId, termsId, ip));
    }

    private Terms getById(Long id) {
        return termsRepository.findById(id)
            .orElseThrow(() -> new TermsException("NOT_FOUND", "약관을 찾을 수 없습니다."));
    }

    private void validateType(String type) {
        if (!Terms.TYPE_TERMS.equals(type) && !Terms.TYPE_PRIVACY.equals(type)) {
            throw new TermsException("INVALID_TYPE", "유효하지 않은 약관 유형입니다.");
        }
    }

    public static class TermsException extends RuntimeException {
        private final String code;
        public TermsException(String code, String message) { super(message); this.code = code; }
        public String getCode() { return code; }
    }
}
