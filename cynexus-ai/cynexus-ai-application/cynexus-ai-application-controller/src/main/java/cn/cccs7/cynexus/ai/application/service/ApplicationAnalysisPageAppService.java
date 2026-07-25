package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisPageDTO;
import cn.cccs7.cynexus.ai.api.request.ApplicationAnalysisPageRequest;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisPageDomainService;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAnalysisPageAppService {

    private final ApplicationAnalysisPageDomainService domainService;

    public ApplicationAnalysisPageAppService(ApplicationAnalysisPageDomainService domainService) {
        this.domainService = domainService;
    }

    public ApplicationAnalysisPageDTO.TargetsResponse getTargets() {
        return toTargetsResponse(domainService.getTargets());
    }

    public ApplicationAnalysisPageDTO.TargetsResponse saveTargets(ApplicationAnalysisPageRequest.SaveTargetsRequest request) {
        ApplicationAnalysisPageRequest.Horizon horizon = request == null ? null : request.getHorizon();
        List<ApplicationAnalysisPageDomainService.TargetInput> targets = request == null || request.getItems() == null
                ? List.of() : request.getItems().stream().map(this::toTargetInput).toList();
        return toTargetsResponse(domainService.saveTargets(new ApplicationAnalysisPageDomainService.SaveTargetsCommand(
                horizon == null ? null : horizon.getDays(), horizon == null ? null : horizon.getSegments(),
                horizon == null ? null : horizon.getMonthlyKeep(), horizon == null ? null : horizon.getWeeklyKeep(), targets)));
    }

    public ApplicationAnalysisPageDTO.AnalysisResultResponse getResult(String targetId) {
        return toResultResponse(domainService.getResult(targetId));
    }

    public ApplicationAnalysisPageDTO.ActionResponse trigger(String targetId) {
        return toActionResponse(domainService.trigger(targetId));
    }

    public ApplicationAnalysisPageDTO.SchedulerStatus getSchedulerStatus() {
        return toSchedulerStatus(domainService.getSchedulerStatus());
    }

    public ApplicationAnalysisPageDTO.SchedulerControlResponse controlScheduler(String action) {
        ApplicationAnalysisPageDomainService.ControlView view = domainService.controlScheduler(action);
        return ApplicationAnalysisPageDTO.SchedulerControlResponse.builder().ok(view.ok())
                .status(toSchedulerStatus(view.status())).error(view.error()).build();
    }

    public ApplicationAnalysisPageDTO.DailySnapshotResponse refreshRecent30(ApplicationAnalysisPageRequest.Recent30RefreshRequest request) {
        return toDailySnapshotResponse(domainService.refreshRecent30(new ApplicationAnalysisPageDomainService.Recent30Command(
                request == null ? null : request.getTargetId(), request == null ? null : request.getDate())));
    }

    public ApplicationAnalysisPageDTO.AnalysisResultResponse run(ApplicationAnalysisPageRequest.RunRequest request) {
        return toResultResponse(domainService.run(new ApplicationAnalysisPageDomainService.RunCommand(
                request == null ? null : request.getTargetType(), request == null ? null : request.getSymbol(),
                request == null ? null : request.getName(), request == null ? null : request.getAdjust())));
    }

    public ApplicationAnalysisPageDTO.DailySnapshotResponse listRecent30(String targetId, int limit) {
        return toDailySnapshotResponse(domainService.listRecent30(targetId, limit));
    }

    public ApplicationAnalysisPageDTO.Recent30FullResponse listRecent30Full(String targetId, int limit) {
        ApplicationAnalysisPageDomainService.FullView view = domainService.listRecent30Full(targetId, limit);
        return ApplicationAnalysisPageDTO.Recent30FullResponse.builder().ok(view.ok()).targetId(view.targetId())
                .error(view.error()).items(view.items().stream().map(item -> ApplicationAnalysisPageDTO.Recent30FullItem.builder()
                        .filename(item.filename()).path(item.path()).date(item.date().toString()).sizeBytes(item.sizeBytes())
                        .updatedAt(item.updatedAt()).snapshot(toSnapshotDetail(item.snapshot())).build()).toList()).build();
    }

    public ApplicationAnalysisPageDTO.DailySnapshotResponse readRecent30(String targetId, LocalDate tradeDate) {
        return toDailySnapshotResponse(domainService.readRecent30(targetId, tradeDate));
    }

    public String unsupportedExecutorMessage() {
        return "Java application-analysis recent30 executor is connected; full annotation scheduler remains read-only.";
    }

    private ApplicationAnalysisPageDomainService.TargetInput toTargetInput(ApplicationAnalysisPageRequest.Target target) {
        return new ApplicationAnalysisPageDomainService.TargetInput(target.getId(), target.getTargetType(), target.getSymbol(),
                target.getMarket(), target.getName(), target.getAdjust(), target.getEnabled(), target.getIntervalMinutes(), target.getTags());
    }

    private ApplicationAnalysisPageDTO.TargetsResponse toTargetsResponse(ApplicationAnalysisPageDomainService.TargetsView view) {
        List<ApplicationAnalysisPageDTO.Target> targets = view.targets().stream().map(this::toTarget).toList();
        ApplicationAnalysisPageBO.Config config = view.config();
        ApplicationAnalysisPageDTO.Horizon horizon = config == null ? ApplicationAnalysisPageDTO.Horizon.builder().days(120).segments(4).monthlyKeep(6).weeklyKeep(12).build()
                : ApplicationAnalysisPageDTO.Horizon.builder().days(config.getHorizonDays()).segments(config.getHorizonSegments())
                .monthlyKeep(config.getMonthlyKeep()).weeklyKeep(config.getWeeklyKeep()).build();
        return ApplicationAnalysisPageDTO.TargetsResponse.builder().items(targets)
                .config(ApplicationAnalysisPageDTO.ConfigPayload.builder().version(1).updatedAt(config == null ? null : config.getUpdatedAt())
                        .horizon(horizon).items(targets).build()).build();
    }

    private ApplicationAnalysisPageDTO.Target toTarget(ApplicationAnalysisPageDomainService.TargetView view) {
        ApplicationAnalysisPageBO.Target target = view.target();
        return ApplicationAnalysisPageDTO.Target.builder().id(target.getTargetKey()).targetType(defaultString(target.getTargetType(), "stock"))
                .symbol(defaultString(target.getSymbol(), "")).market(target.getMarket()).name(defaultString(target.getName(), target.getSymbol()))
                .adjust(defaultString(target.getAdjust(), "qfq")).enabled(target.getEnabled()).intervalMinutes(target.getIntervalMinutes())
                .tags(view.tags()).lastUpdatedAt(target.getUpdatedAt())
                .lastResultPath(view.hasCurrentResult() ? "postgres://cynexus_appl_ai.application_analysis_result_current/" + target.getTargetKey() : null).build();
    }

    private ApplicationAnalysisPageDTO.AnalysisResultResponse toResultResponse(ApplicationAnalysisPageDomainService.ResultView view) {
        if (view == null) return null;
        return ApplicationAnalysisPageDTO.AnalysisResultResponse.builder().analysisInputJson(view.analysisInputJson())
                .analysisResultJson(view.analysisResultJson()).rawResultJson(view.rawResultJson())
                .metaResultPath("postgres://cynexus_appl_ai.application_analysis_result_current/" + view.targetKey())
                .metaHistory(view.history().stream().map(history -> ApplicationAnalysisPageDTO.ResultFile.builder()
                        .filename(history.filename()).path(history.path()).sizeBytes(history.sizeBytes()).updatedAt(history.updatedAt()).build()).toList()).build();
    }

    private ApplicationAnalysisPageDTO.ActionResponse toActionResponse(ApplicationAnalysisPageDomainService.ActionView view) {
        return ApplicationAnalysisPageDTO.ActionResponse.builder().ok(view.ok()).targetId(view.targetId()).error(view.error())
                .items(view.items().stream().map(item -> ApplicationAnalysisPageDTO.ActionItem.builder().id(item.id()).ok(item.ok()).error(item.error()).build()).toList()).build();
    }

    private ApplicationAnalysisPageDTO.SchedulerStatus toSchedulerStatus(ApplicationAnalysisPageDomainService.SchedulerStatusView view) {
        return ApplicationAnalysisPageDTO.SchedulerStatus.builder().running(view.running()).startedAt(view.startedAt()).tickCount(view.tickCount())
                .runsCount(view.runsCount()).enabledTargetCount(view.enabledTargetCount()).totalTargetCount(view.totalTargetCount())
                .inflight(view.inflight()).lastRun(view.lastRun()).build();
    }

    private ApplicationAnalysisPageDTO.DailySnapshotResponse toDailySnapshotResponse(ApplicationAnalysisPageDomainService.DailySnapshotView view) {
        return ApplicationAnalysisPageDTO.DailySnapshotResponse.builder().ok(view.ok()).error(view.error()).targetId(view.targetId())
                .date(view.date() == null ? null : view.date().toString()).snapshotPath(view.snapshotPath())
                .shortTermTrendJson(view.shortTermTrendJson()).currentSituationJson(view.currentSituationJson())
                .updated(view.updated()).skipReason(view.skipReason()).snapshots(view.snapshots().stream().map(file -> ApplicationAnalysisPageDTO.DailySnapshotFile.builder()
                        .filename(file.filename()).path(file.path()).date(file.date().toString()).sizeBytes(file.sizeBytes()).updatedAt(file.updatedAt()).build()).toList())
                .snapshot(view.snapshot() == null ? null : toSnapshotDetail(view.snapshot())).build();
    }

    private ApplicationAnalysisPageDTO.DailySnapshotDetail toSnapshotDetail(ApplicationAnalysisPageDomainService.SnapshotDetail detail) {
        return ApplicationAnalysisPageDTO.DailySnapshotDetail.builder().target(toTarget(new ApplicationAnalysisPageDomainService.TargetView(detail.target(), false, detail.tags())))
                .date(detail.snapshot().getTradeDate().toString()).updatedAt(detail.snapshot().getUpdatedAt())
                .shortTermTrendJson(detail.shortTermTrendJson()).currentSituationJson(detail.currentSituationJson()).summaryJson(detail.summaryJson()).build();
    }

    private String defaultString(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
}
