package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisPageRepository;
import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisConfigPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisDailySnapshotPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisResultCurrentPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisResultHistoryPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisTargetPO;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisConfigBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisDailySnapshotBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisResultCurrentBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisResultHistoryBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisTargetBasicService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationAnalysisPageRepositoryImpl implements ApplicationAnalysisPageRepository {

    private static final String DEFAULT_CONFIG_KEY = "default";

    private final ApplicationAnalysisConfigBasicService configBasicService;
    private final ApplicationAnalysisTargetBasicService targetBasicService;
    private final ApplicationAnalysisResultCurrentBasicService currentBasicService;
    private final ApplicationAnalysisResultHistoryBasicService historyBasicService;
    private final ApplicationAnalysisDailySnapshotBasicService dailySnapshotBasicService;

    public ApplicationAnalysisPageRepositoryImpl(
            ApplicationAnalysisConfigBasicService configBasicService,
            ApplicationAnalysisTargetBasicService targetBasicService,
            ApplicationAnalysisResultCurrentBasicService currentBasicService,
            ApplicationAnalysisResultHistoryBasicService historyBasicService,
            ApplicationAnalysisDailySnapshotBasicService dailySnapshotBasicService) {
        this.configBasicService = configBasicService;
        this.targetBasicService = targetBasicService;
        this.currentBasicService = currentBasicService;
        this.historyBasicService = historyBasicService;
        this.dailySnapshotBasicService = dailySnapshotBasicService;
    }

    @Override
    public ApplicationAnalysisPageBO.Config getDefaultConfig() {
        return toConfig(ensureDefaultConfig());
    }

    @Override
    public List<ApplicationAnalysisPageBO.Target> listTargets() {
        return targetBasicService.listActiveTargets().stream()
                .map(this::toTarget)
                .toList();
    }

    @Override
    public ApplicationAnalysisPageBO.Target findTarget(String targetKey) {
        ApplicationAnalysisTargetPO po = targetBasicService.getActiveByTargetKey(normalizeTargetKey(targetKey));
        return toTarget(po);
    }

    @Override
    public ApplicationAnalysisPageBO.Config saveTargets(ApplicationAnalysisPageBO.SaveTargetsCommand command) {
        OffsetDateTime now = OffsetDateTime.now();
        ApplicationAnalysisConfigPO config = ensureDefaultConfig();
        config.setHorizonDays(clamp(command.getHorizonDays(), 30, config.getHorizonDays(), 120));
        config.setHorizonSegments(clamp(command.getHorizonSegments(), 1, config.getHorizonSegments(), 4));
        config.setMonthlyKeep(clamp(command.getMonthlyKeep(), 1, config.getMonthlyKeep(), 6));
        config.setWeeklyKeep(clamp(command.getWeeklyKeep(), 1, config.getWeeklyKeep(), 12));
        config.setUpdatedAt(now);
        configBasicService.update(config);

        List<ApplicationAnalysisTargetPO> aliveTargets = targetBasicService.listActiveTargets();
        Map<String, ApplicationAnalysisTargetPO> targetByKey = aliveTargets.stream()
                .collect(Collectors.toMap(ApplicationAnalysisTargetPO::getTargetKey, Function.identity(), (left, right) -> left));
        List<String> seenKeys = new ArrayList<>();
        List<ApplicationAnalysisPageBO.Target> incomingTargets =
                command.getTargets() == null ? List.of() : command.getTargets();

        for (int index = 0; index < incomingTargets.size(); index++) {
            ApplicationAnalysisPageBO.Target incoming = incomingTargets.get(index);
            String targetType = defaultString(incoming.getTargetType(), "stock").toLowerCase(Locale.ROOT);
            String symbol = defaultString(incoming.getSymbol(), "").toUpperCase(Locale.ROOT);
            if (symbol.isBlank()) {
                continue;
            }
            String targetKey = normalizeTargetKey(targetType + "-" + symbol);
            seenKeys.add(targetKey);
            ApplicationAnalysisTargetPO existing = targetByKey.get(targetKey);
            if (existing == null) {
                existing = new ApplicationAnalysisTargetPO();
                existing.setTargetKey(targetKey);
                existing.setLegacyKey(defaultString(incoming.getLegacyKey(), targetKey));
                existing.setSourceType("manual");
                existing.setStatus("active");
            }
            existing.setSymbol(symbol);
            existing.setMarket(blankToNull(incoming.getMarket()));
            existing.setName(defaultString(incoming.getName(), symbol));
            existing.setTargetType(targetType);
            existing.setAdjust(defaultString(incoming.getAdjust(), "qfq"));
            existing.setEnabled(incoming.getEnabled() == null || incoming.getEnabled());
            existing.setIntervalMinutes(Math.max(5, incoming.getIntervalMinutes() == null ? 60 : incoming.getIntervalMinutes()));
            existing.setSortOrder(index + 1);
            existing.setTags(defaultString(incoming.getTagsJson(), "[]"));
            existing.setUpdatedAt(now);
            if (existing.getId() == null) {
                targetBasicService.create(existing);
            } else {
                targetBasicService.update(existing);
            }
        }

        targetBasicService.softDeleteMissing(seenKeys, now);
        return toConfig(config);
    }

    @Override
    public ApplicationAnalysisPageBO.CurrentResult findCurrentResult(String targetKey) {
        return toCurrentResult(currentBasicService.getActiveByTargetKey(normalizeTargetKey(targetKey)));
    }

    @Override
    public List<ApplicationAnalysisPageBO.HistoryResult> listHistory(String targetKey, int limit) {
        return historyBasicService.listActiveByTargetKey(normalizeTargetKey(targetKey), normalizeLimit(limit)).stream()
                .map(this::toHistoryResult)
                .toList();
    }

    @Override
    public List<ApplicationAnalysisPageBO.DailySnapshot> listDailySnapshots(String targetKey, int limit) {
        return dailySnapshotBasicService.listRecent30ByTargetKey(normalizeTargetKey(targetKey), normalizeLimit(limit))
                .stream()
                .map(this::toDailySnapshot)
                .toList();
    }

    @Override
    public ApplicationAnalysisPageBO.DailySnapshot findDailySnapshot(String targetKey, LocalDate tradeDate) {
        if (tradeDate == null) {
            return null;
        }
        return toDailySnapshot(dailySnapshotBasicService.getRecent30ByTargetKeyAndDate(
                normalizeTargetKey(targetKey), tradeDate));
    }

    private ApplicationAnalysisConfigPO ensureDefaultConfig() {
        ApplicationAnalysisConfigPO config = configBasicService.getDefaultActive();
        if (config != null) {
            return config;
        }
        config = new ApplicationAnalysisConfigPO();
        config.setConfigKey(DEFAULT_CONFIG_KEY);
        config.setStatus("active");
        config.setHorizonDays(120);
        config.setHorizonSegments(4);
        config.setMonthlyKeep(6);
        config.setWeeklyKeep(12);
        config.setExtra("{}");
        configBasicService.create(config);
        return config;
    }

    private ApplicationAnalysisPageBO.Config toConfig(ApplicationAnalysisConfigPO po) {
        if (po == null) {
            return null;
        }
        return ApplicationAnalysisPageBO.Config.builder()
                .id(po.getId())
                .horizonDays(po.getHorizonDays())
                .horizonSegments(po.getHorizonSegments())
                .monthlyKeep(po.getMonthlyKeep())
                .weeklyKeep(po.getWeeklyKeep())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private ApplicationAnalysisPageBO.Target toTarget(ApplicationAnalysisTargetPO po) {
        if (po == null) {
            return null;
        }
        return ApplicationAnalysisPageBO.Target.builder()
                .id(po.getId())
                .targetKey(po.getTargetKey())
                .legacyKey(po.getLegacyKey())
                .targetType(po.getTargetType())
                .symbol(po.getSymbol())
                .market(po.getMarket())
                .name(po.getName())
                .adjust(po.getAdjust())
                .enabled(po.getEnabled())
                .intervalMinutes(po.getIntervalMinutes())
                .sortOrder(po.getSortOrder())
                .tagsJson(po.getTags())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private ApplicationAnalysisPageBO.CurrentResult toCurrentResult(ApplicationAnalysisResultCurrentPO po) {
        if (po == null) {
            return null;
        }
        return ApplicationAnalysisPageBO.CurrentResult.builder()
                .targetKey(po.getTargetKey())
                .payloadJson(po.getPayload())
                .analysisRunAt(po.getAnalysisRunAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private ApplicationAnalysisPageBO.HistoryResult toHistoryResult(ApplicationAnalysisResultHistoryPO po) {
        return ApplicationAnalysisPageBO.HistoryResult.builder()
                .targetKey(po.getTargetKey())
                .historyKey(po.getHistoryKey())
                .payloadJson(po.getPayload())
                .analysisRunAt(po.getAnalysisRunAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private ApplicationAnalysisPageBO.DailySnapshot toDailySnapshot(ApplicationAnalysisDailySnapshotPO po) {
        if (po == null) {
            return null;
        }
        return ApplicationAnalysisPageBO.DailySnapshot.builder()
                .targetKey(po.getTargetKey())
                .tradeDate(po.getTradeDate())
                .payloadJson(po.getPayload())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private String normalizeTargetKey(String value) {
        return defaultString(value, "").trim().toLowerCase(Locale.ROOT);
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 200));
    }

    private int clamp(Integer value, int min, Integer fallback, int defaultValue) {
        int resolved = value == null ? (fallback == null ? defaultValue : fallback) : value;
        return Math.max(min, resolved);
    }

    private String defaultString(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
