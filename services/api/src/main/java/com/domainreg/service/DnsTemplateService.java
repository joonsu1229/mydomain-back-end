package com.domainreg.service;

import com.domainreg.core.entity.DnsTemplate;
import com.domainreg.core.port.DnsTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DnsTemplateService {

    private final DnsTemplateRepository templateRepository;

    public DnsTemplateService(DnsTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<DnsTemplate> getTemplates(Long userId) {
        return templateRepository.findByUserId(userId);
    }

    @Transactional
    public DnsTemplate createTemplate(Long userId, String name, String description, String recordsJson) {
        DnsTemplate t = DnsTemplate.create(userId, name, description, recordsJson);
        return templateRepository.save(t);
    }

    @Transactional
    public DnsTemplate updateTemplate(Long userId, Long templateId,
                                       String name, String description, String recordsJson) {
        DnsTemplate t = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        if (!t.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        t.setName(name);
        t.setDescription(description);
        t.setRecordsJson(recordsJson);
        return templateRepository.save(t);
    }

    @Transactional
    public void deleteTemplate(Long userId, Long templateId) {
        DnsTemplate t = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        if (!t.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        templateRepository.deleteById(templateId);
    }
}
