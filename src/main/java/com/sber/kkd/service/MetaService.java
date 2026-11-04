package com.sber.kkd.service;

import com.sber.kkd.api.dto.UnitOptionDto;
import com.sber.kkd.domain.KkdRule;
import com.sber.kkd.domain.Unit;
import com.sber.kkd.repository.KkdRuleRepository;
import com.sber.kkd.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Отдаёт справочные данные для фронтенда: подразделения с иерархией и правила ККД.
 */
@Service
public class MetaService {

    private final UnitRepository unitRepository;
    private final KkdRuleRepository ruleRepository;

    public MetaService(UnitRepository unitRepository, KkdRuleRepository ruleRepository) {
        this.unitRepository = unitRepository;
        this.ruleRepository = ruleRepository;
    }

    @Transactional(readOnly = true)
    public List<UnitOptionDto> units() {
        return unitRepository.findAll().stream()
                .map(u -> new UnitOptionDto(u.getId(), u.getCode(), u.getName(), u.getParentId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KkdRule> rules() {
        return ruleRepository.findAll();
    }
}