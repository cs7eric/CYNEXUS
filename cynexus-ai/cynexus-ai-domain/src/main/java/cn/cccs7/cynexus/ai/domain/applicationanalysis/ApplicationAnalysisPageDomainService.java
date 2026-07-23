package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import java.time.LocalDate;
import java.util.List;

public interface ApplicationAnalysisPageDomainService {

    TargetsView getTargets();

    TargetsView saveTargets(SaveTargetsCommand command);

    ResultView getResult(String targetId);

    ActionView trigger(String targetId);

    SchedulerStatusView getSchedulerStatus();

    ControlView controlScheduler(String action);

    DailySnapshotView refreshRecent30(Recent30Command command);

    ResultView run(RunCommand command);

    DailySnapshotView listRecent30(String targetId, int limit);

    FullView listRecent30Full(String targetId, int limit);

    DailySnapshotView readRecent30(String targetId, LocalDate tradeDate);

    record SaveTargetsCommand(Integer horizonDays, Integer horizonSegments, Integer monthlyKeep,
                              Integer weeklyKeep, List<TargetInput> targets) {
    }

    record TargetInput(String id, String targetType, String symbol, String market, String name,
                       String adjust, Boolean enabled, Integer intervalMinutes, List<String> tags) {
    }

    record RunCommand(String targetType, String symbol, String name, String adjust) {
    }

    record Recent30Command(String targetId, String date) {
    }

    record TargetsView(ApplicationAnalysisPageBO.Config config, List<TargetView> targets) {
    }

    record TargetView(ApplicationAnalysisPageBO.Target target, boolean hasCurrentResult) {
    }

    record ResultView(String targetKey, String analysisInputJson, String analysisResultJson,
                     String rawResultJson, List<HistoryFile> history) {
    }

    record HistoryFile(String filename, String path, Integer sizeBytes,
                       java.time.OffsetDateTime updatedAt) {
    }

    record ActionView(boolean ok, String targetId, String error, List<ActionItem> items) {
    }

    record ActionItem(String id, boolean ok, String error) {
    }

    record SchedulerStatusView(boolean running, java.time.OffsetDateTime startedAt, int tickCount,
                               int runsCount, int enabledTargetCount, int totalTargetCount,
                               String inflight, String lastRun) {
    }

    record ControlView(boolean ok, SchedulerStatusView status, String error) {
    }

    record DailySnapshotView(boolean ok, String error, String targetId, LocalDate date,
                             String snapshotPath, String shortTermTrendJson,
                             String currentSituationJson, Boolean updated, String skipReason,
                             List<SnapshotFile> snapshots, SnapshotDetail snapshot) {
    }

    record SnapshotFile(String filename, String path, LocalDate date, Integer sizeBytes,
                        java.time.OffsetDateTime updatedAt) {
    }

    record SnapshotDetail(ApplicationAnalysisPageBO.Target target, ApplicationAnalysisPageBO.DailySnapshot snapshot,
                          String shortTermTrendJson, String currentSituationJson, String summaryJson) {
    }

    record FullView(boolean ok, String targetId, List<FullItem> items, String error) {
    }

    record FullItem(String filename, String path, LocalDate date, Integer sizeBytes,
                    java.time.OffsetDateTime updatedAt, SnapshotDetail snapshot) {
    }
}
