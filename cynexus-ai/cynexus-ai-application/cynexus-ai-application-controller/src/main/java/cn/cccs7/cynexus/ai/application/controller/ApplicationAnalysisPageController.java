package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisPageDTO;
import cn.cccs7.cynexus.ai.api.request.ApplicationAnalysisPageRequest;
import cn.cccs7.cynexus.ai.application.service.ApplicationAnalysisPageAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/application-analysis")
public class ApplicationAnalysisPageController {

    private final ApplicationAnalysisPageAppService appService;

    public ApplicationAnalysisPageController(ApplicationAnalysisPageAppService appService) {
        this.appService = appService;
    }

    @GetMapping("/targets")
    public Result<ApplicationAnalysisPageDTO.TargetsResponse> getTargets() {
        return Result.success(appService.getTargets());
    }

    @PutMapping("/targets")
    public Result<ApplicationAnalysisPageDTO.TargetsResponse> saveTargets(
            @RequestBody ApplicationAnalysisPageRequest.SaveTargetsRequest request) {
        return Result.success(appService.saveTargets(request));
    }

    @GetMapping("/results/{targetId}")
    public Result<ApplicationAnalysisPageDTO.AnalysisResultResponse> getResult(
            @PathVariable("targetId") String targetId) {
        ApplicationAnalysisPageDTO.AnalysisResultResponse result = appService.getResult(targetId);
        if (result == null) {
            return Result.fail("AI_APPLICATION_ANALYSIS_RESULT_NOT_FOUND",
                    "Application analysis result not found: " + targetId);
        }
        return Result.success(result);
    }

    @PostMapping("/refresh")
    public Result<ApplicationAnalysisPageDTO.ActionResponse> trigger(
            @RequestBody(required = false) ApplicationAnalysisPageRequest.TriggerRequest request) {
        return Result.success(appService.trigger(request == null ? null : request.getTargetId()));
    }

    @PostMapping
    public Result<ApplicationAnalysisPageDTO.AnalysisResultResponse> run(
            @RequestBody ApplicationAnalysisPageRequest.RunRequest request) {
        return Result.success(appService.run(request));
    }

    @GetMapping("/scheduler")
    public Result<ApplicationAnalysisPageDTO.SchedulerStatus> schedulerStatus() {
        return Result.success(appService.getSchedulerStatus());
    }

    @PostMapping("/scheduler/{action}")
    public Result<ApplicationAnalysisPageDTO.SchedulerControlResponse> controlScheduler(
            @PathVariable("action") String action) {
        return Result.success(appService.controlScheduler(action));
    }

    @PostMapping("/recent30/refresh")
    public Result<ApplicationAnalysisPageDTO.DailySnapshotResponse> refreshRecent30(
            @RequestBody ApplicationAnalysisPageRequest.Recent30RefreshRequest request) {
        return Result.success(appService.refreshRecent30(request));
    }

    @GetMapping("/recent30/{targetId}")
    public Result<ApplicationAnalysisPageDTO.DailySnapshotResponse> listRecent30(
            @PathVariable("targetId") String targetId,
            @RequestParam(defaultValue = "60") int limit) {
        return Result.success(appService.listRecent30(targetId, limit));
    }

    @GetMapping("/recent30/{targetId}/full")
    public Result<ApplicationAnalysisPageDTO.Recent30FullResponse> listRecent30Full(
            @PathVariable("targetId") String targetId,
            @RequestParam(defaultValue = "60") int limit) {
        return Result.success(appService.listRecent30Full(targetId, limit));
    }

    @GetMapping("/recent30/{targetId}/{date}")
    public Result<ApplicationAnalysisPageDTO.DailySnapshotResponse> readRecent30(
            @PathVariable("targetId") String targetId,
            @PathVariable("date") LocalDate date) {
        return Result.success(appService.readRecent30(targetId, date));
    }
}
