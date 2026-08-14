package com.academy.mudogroupware.resourceusage.application.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageQueryPort;
import com.academy.mudogroupware.resourceusage.application.query.MonthlyResourceUsageView;
import com.academy.mudogroupware.resourceusage.application.query.ResourceUsageResourceSummaryView;
import com.academy.mudogroupware.resourceusage.application.usecase.GetMonthlyResourceUsageUseCase;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageFeatureSummary;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageRepository;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResourceUsageQueryService implements GetMonthlyResourceUsageUseCase, ResourceUsageQueryPort {

    private final ResourceUsageRepository resourceUsageRepository;

    @Override
    @Transactional(readOnly = true)
    public MonthlyResourceUsageView getMonthlyUsage(YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now();
        LocalDateTime from = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime to = targetMonth.plusMonths(1).atDay(1).atStartOfDay();
        Map<ResourceUsageType, List<ResourceUsageFeatureSummary>> summariesByType =
                resourceUsageRepository.summarizeByFeature(from, to).stream()
                        .collect(Collectors.groupingBy(ResourceUsageFeatureSummary::resourceType));

        List<ResourceUsageResourceSummaryView> resources = Arrays.stream(ResourceUsageType.values())
                .map(type -> toResourceSummary(type, summariesByType.getOrDefault(type, List.of())))
                .filter(summary -> summary.totalAmount() > 0)
                .toList();
        return new MonthlyResourceUsageView(targetMonth, resources);
    }

    private ResourceUsageResourceSummaryView toResourceSummary(ResourceUsageType type,
                                                               List<ResourceUsageFeatureSummary> features) {
        long totalAmount = features.stream()
                .mapToLong(ResourceUsageFeatureSummary::totalAmount)
                .sum();
        return new ResourceUsageResourceSummaryView(type, type.unit(), totalAmount, features);
    }

    @Override
    public long sumByType(ResourceUsageType type) {
        return resourceUsageRepository.sumByType(type);
    }

    @Override
    public long sumByTypeAndPeriod(ResourceUsageType type, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        return resourceUsageRepository.sumByTypeAndPeriod(type, fromInclusive, toExclusive);
    }
}
