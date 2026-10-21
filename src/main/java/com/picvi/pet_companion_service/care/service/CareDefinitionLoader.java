package com.picvi.pet_companion_service.care.service;

import com.picvi.pet_companion_service.care.domain.CareItemDefinition;
import com.picvi.pet_companion_service.care.domain.CareStepDefinition;
import com.picvi.pet_companion_service.care.entity.CareItem;
import com.picvi.pet_companion_service.care.entity.CareProtocolStep;
import com.picvi.pet_companion_service.care.repository.CareItemRepository;
import com.picvi.pet_companion_service.care.repository.CareProtocolStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CareDefinitionLoader {
    private final CareItemRepository itemRepository;
    private final CareProtocolStepRepository stepRepository;

    public List<CareItemDefinition> loadAll() {
        Map<String, List<CareStepDefinition>> stepsByItem = this.stepRepository.findAllByOrderByItemCodeAscStepOrderAsc().stream()
                .collect(Collectors.groupingBy(CareProtocolStep::getItemCode, Collectors.mapping(this::toStep, Collectors.toList())));

        return this.itemRepository.findAllByActiveTrueOrderBySortOrder().stream()
                .map(item -> this.toDefinition(item, stepsByItem.getOrDefault(item.getCode(), List.of())))
                .toList();
    }

    public CareItemDefinition load(CareItem item) {
        List<CareStepDefinition> steps = this.stepRepository.findAllByItemCodeOrderByStepOrderAsc(item.getCode()).stream()
                .map(this::toStep)
                .toList();
        return this.toDefinition(item, steps);
    }

    private CareItemDefinition toDefinition(CareItem item, List<CareStepDefinition> steps) {
        return new CareItemDefinition(
                item.getCode(),
                item.getCategory(),
                item.getRequirement(),
                item.isIntervalEditable(),
                item.getLegalMinAgeMonths(),
                item.getLegalWindowDays(),
                item.getLegalRepeatMonths(),
                steps
        );
    }

    private CareStepDefinition toStep(CareProtocolStep step) {
        return new CareStepDefinition(
                step.getKind(),
                step.getMinAgeWeeks(),
                step.getMaxAgeWeeks(),
                step.getGapDays(),
                step.getSeasonStartMonth(),
                step.getSeasonEndMonth()
        );
    }
}
