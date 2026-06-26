package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AiProviderDTO;
import cn.cccs7.cynexus.ai.api.dto.AiUsageBindingDTO;
import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisConfigDTO;
import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisDailySnapshotDTO;
import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisResultCurrentDTO;
import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisResultHistoryDTO;
import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisTargetDTO;
import cn.cccs7.cynexus.ai.application.convert.AiProviderDTOConverter;
import cn.cccs7.cynexus.ai.application.convert.AiUsageBindingDTOConverter;
import cn.cccs7.cynexus.ai.application.convert.ApplicationAnalysisConfigDTOConverter;
import cn.cccs7.cynexus.ai.application.convert.ApplicationAnalysisDailySnapshotDTOConverter;
import cn.cccs7.cynexus.ai.application.convert.ApplicationAnalysisResultCurrentDTOConverter;
import cn.cccs7.cynexus.ai.application.convert.ApplicationAnalysisResultHistoryDTOConverter;
import cn.cccs7.cynexus.ai.application.convert.ApplicationAnalysisTargetDTOConverter;
import cn.cccs7.cynexus.ai.application.service.AiConfigurationCrudAppService;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisConfigBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisDailySnapshotBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultCurrentBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultHistoryBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisTargetBO;
import cn.cccs7.cynexus.common.core.result.Result;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/config")
public class AiConfigurationCrudController {

    private final AiConfigurationCrudAppService appService;

    public AiConfigurationCrudController(AiConfigurationCrudAppService appService) {
        this.appService = appService;
    }

    @GetMapping("/providers")
    public Result<List<AiProviderDTO>> listProviders() {
        return Result.success(AiProviderDTOConverter.INSTANCE.toSourceList(appService.listProviders()));
    }

    @GetMapping("/providers/{id}")
    public Result<AiProviderDTO> getProvider(@PathVariable Long id) {
        AiProviderBO provider = appService.findProviderById(id);
        return Result.success(provider == null ? null : AiProviderDTOConverter.INSTANCE.toSource(provider));
    }

    @PostMapping("/providers")
    public Result<Long> createProvider(@RequestBody AiProviderDTO dto) {
        return Result.success(appService.createProvider(AiProviderDTOConverter.INSTANCE.toTarget(dto)));
    }

    @PutMapping("/providers")
    public Result<Boolean> updateProvider(@RequestBody AiProviderDTO dto) {
        return Result.success(appService.updateProvider(AiProviderDTOConverter.INSTANCE.toTarget(dto)));
    }

    @DeleteMapping("/providers/{id}")
    public Result<Boolean> deleteProvider(@PathVariable Long id) {
        return Result.success(appService.deleteProvider(id));
    }

    @GetMapping("/usage-bindings")
    public Result<List<AiUsageBindingDTO>> listUsageBindings() {
        return Result.success(AiUsageBindingDTOConverter.INSTANCE.toSourceList(appService.listUsageBindings()));
    }

    @GetMapping("/usage-bindings/{id}")
    public Result<AiUsageBindingDTO> getUsageBinding(@PathVariable Long id) {
        AiUsageBindingBO binding = appService.findUsageBindingById(id);
        return Result.success(binding == null ? null : AiUsageBindingDTOConverter.INSTANCE.toSource(binding));
    }

    @PostMapping("/usage-bindings")
    public Result<Long> createUsageBinding(@RequestBody AiUsageBindingDTO dto) {
        return Result.success(appService.createUsageBinding(AiUsageBindingDTOConverter.INSTANCE.toTarget(dto)));
    }

    @PutMapping("/usage-bindings")
    public Result<Boolean> updateUsageBinding(@RequestBody AiUsageBindingDTO dto) {
        return Result.success(appService.updateUsageBinding(AiUsageBindingDTOConverter.INSTANCE.toTarget(dto)));
    }

    @DeleteMapping("/usage-bindings/{id}")
    public Result<Boolean> deleteUsageBinding(@PathVariable Long id) {
        return Result.success(appService.deleteUsageBinding(id));
    }

    @GetMapping("/analysis-configs")
    public Result<List<ApplicationAnalysisConfigDTO>> listAnalysisConfigs() {
        return Result.success(ApplicationAnalysisConfigDTOConverter.INSTANCE.toSourceList(
                appService.listAnalysisConfigs()));
    }

    @GetMapping("/analysis-configs/{id}")
    public Result<ApplicationAnalysisConfigDTO> getAnalysisConfig(@PathVariable Long id) {
        ApplicationAnalysisConfigBO config = appService.findAnalysisConfigById(id);
        return Result.success(config == null ? null : ApplicationAnalysisConfigDTOConverter.INSTANCE.toSource(config));
    }

    @PostMapping("/analysis-configs")
    public Result<Long> createAnalysisConfig(@RequestBody ApplicationAnalysisConfigDTO dto) {
        return Result.success(appService.createAnalysisConfig(
                ApplicationAnalysisConfigDTOConverter.INSTANCE.toTarget(dto)));
    }

    @PutMapping("/analysis-configs")
    public Result<Boolean> updateAnalysisConfig(@RequestBody ApplicationAnalysisConfigDTO dto) {
        return Result.success(appService.updateAnalysisConfig(
                ApplicationAnalysisConfigDTOConverter.INSTANCE.toTarget(dto)));
    }

    @DeleteMapping("/analysis-configs/{id}")
    public Result<Boolean> deleteAnalysisConfig(@PathVariable Long id) {
        return Result.success(appService.deleteAnalysisConfig(id));
    }

    @GetMapping("/analysis-daily-snapshots")
    public Result<List<ApplicationAnalysisDailySnapshotDTO>> listAnalysisDailySnapshots() {
        return Result.success(ApplicationAnalysisDailySnapshotDTOConverter.INSTANCE.toSourceList(
                appService.listAnalysisDailySnapshots()));
    }

    @GetMapping("/analysis-daily-snapshots/{id}")
    public Result<ApplicationAnalysisDailySnapshotDTO> getAnalysisDailySnapshot(@PathVariable Long id) {
        ApplicationAnalysisDailySnapshotBO snapshot = appService.findAnalysisDailySnapshotById(id);
        return Result.success(snapshot == null ? null
                : ApplicationAnalysisDailySnapshotDTOConverter.INSTANCE.toSource(snapshot));
    }

    @PostMapping("/analysis-daily-snapshots")
    public Result<Long> createAnalysisDailySnapshot(@RequestBody ApplicationAnalysisDailySnapshotDTO dto) {
        return Result.success(appService.createAnalysisDailySnapshot(
                ApplicationAnalysisDailySnapshotDTOConverter.INSTANCE.toTarget(dto)));
    }

    @PutMapping("/analysis-daily-snapshots")
    public Result<Boolean> updateAnalysisDailySnapshot(@RequestBody ApplicationAnalysisDailySnapshotDTO dto) {
        return Result.success(appService.updateAnalysisDailySnapshot(
                ApplicationAnalysisDailySnapshotDTOConverter.INSTANCE.toTarget(dto)));
    }

    @DeleteMapping("/analysis-daily-snapshots/{id}")
    public Result<Boolean> deleteAnalysisDailySnapshot(@PathVariable Long id) {
        return Result.success(appService.deleteAnalysisDailySnapshot(id));
    }

    @GetMapping("/analysis-result-current")
    public Result<List<ApplicationAnalysisResultCurrentDTO>> listAnalysisResultCurrent() {
        return Result.success(ApplicationAnalysisResultCurrentDTOConverter.INSTANCE.toSourceList(
                appService.listAnalysisResultCurrent()));
    }

    @GetMapping("/analysis-result-current/{id}")
    public Result<ApplicationAnalysisResultCurrentDTO> getAnalysisResultCurrent(@PathVariable Long id) {
        ApplicationAnalysisResultCurrentBO result = appService.findAnalysisResultCurrentById(id);
        return Result.success(result == null ? null
                : ApplicationAnalysisResultCurrentDTOConverter.INSTANCE.toSource(result));
    }

    @PostMapping("/analysis-result-current")
    public Result<Long> createAnalysisResultCurrent(@RequestBody ApplicationAnalysisResultCurrentDTO dto) {
        return Result.success(appService.createAnalysisResultCurrent(
                ApplicationAnalysisResultCurrentDTOConverter.INSTANCE.toTarget(dto)));
    }

    @PutMapping("/analysis-result-current")
    public Result<Boolean> updateAnalysisResultCurrent(@RequestBody ApplicationAnalysisResultCurrentDTO dto) {
        return Result.success(appService.updateAnalysisResultCurrent(
                ApplicationAnalysisResultCurrentDTOConverter.INSTANCE.toTarget(dto)));
    }

    @DeleteMapping("/analysis-result-current/{id}")
    public Result<Boolean> deleteAnalysisResultCurrent(@PathVariable Long id) {
        return Result.success(appService.deleteAnalysisResultCurrent(id));
    }

    @GetMapping("/analysis-result-history")
    public Result<List<ApplicationAnalysisResultHistoryDTO>> listAnalysisResultHistory() {
        return Result.success(ApplicationAnalysisResultHistoryDTOConverter.INSTANCE.toSourceList(
                appService.listAnalysisResultHistory()));
    }

    @GetMapping("/analysis-result-history/{id}")
    public Result<ApplicationAnalysisResultHistoryDTO> getAnalysisResultHistory(@PathVariable Long id) {
        ApplicationAnalysisResultHistoryBO result = appService.findAnalysisResultHistoryById(id);
        return Result.success(result == null ? null
                : ApplicationAnalysisResultHistoryDTOConverter.INSTANCE.toSource(result));
    }

    @PostMapping("/analysis-result-history")
    public Result<Long> createAnalysisResultHistory(@RequestBody ApplicationAnalysisResultHistoryDTO dto) {
        return Result.success(appService.createAnalysisResultHistory(
                ApplicationAnalysisResultHistoryDTOConverter.INSTANCE.toTarget(dto)));
    }

    @PutMapping("/analysis-result-history")
    public Result<Boolean> updateAnalysisResultHistory(@RequestBody ApplicationAnalysisResultHistoryDTO dto) {
        return Result.success(appService.updateAnalysisResultHistory(
                ApplicationAnalysisResultHistoryDTOConverter.INSTANCE.toTarget(dto)));
    }

    @DeleteMapping("/analysis-result-history/{id}")
    public Result<Boolean> deleteAnalysisResultHistory(@PathVariable Long id) {
        return Result.success(appService.deleteAnalysisResultHistory(id));
    }

    @GetMapping("/analysis-targets")
    public Result<List<ApplicationAnalysisTargetDTO>> listAnalysisTargets() {
        return Result.success(ApplicationAnalysisTargetDTOConverter.INSTANCE.toSourceList(
                appService.listAnalysisTargets()));
    }

    @GetMapping("/analysis-targets/{id}")
    public Result<ApplicationAnalysisTargetDTO> getAnalysisTarget(@PathVariable Long id) {
        ApplicationAnalysisTargetBO target = appService.findAnalysisTargetById(id);
        return Result.success(target == null ? null : ApplicationAnalysisTargetDTOConverter.INSTANCE.toSource(target));
    }

    @PostMapping("/analysis-targets")
    public Result<Long> createAnalysisTarget(@RequestBody ApplicationAnalysisTargetDTO dto) {
        return Result.success(appService.createAnalysisTarget(
                ApplicationAnalysisTargetDTOConverter.INSTANCE.toTarget(dto)));
    }

    @PutMapping("/analysis-targets")
    public Result<Boolean> updateAnalysisTarget(@RequestBody ApplicationAnalysisTargetDTO dto) {
        return Result.success(appService.updateAnalysisTarget(
                ApplicationAnalysisTargetDTOConverter.INSTANCE.toTarget(dto)));
    }

    @DeleteMapping("/analysis-targets/{id}")
    public Result<Boolean> deleteAnalysisTarget(@PathVariable Long id) {
        return Result.success(appService.deleteAnalysisTarget(id));
    }
}
