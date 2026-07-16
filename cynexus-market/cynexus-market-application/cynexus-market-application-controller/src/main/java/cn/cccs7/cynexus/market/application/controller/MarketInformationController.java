package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketInformationDTO;
import cn.cccs7.cynexus.market.application.service.MarketInformationAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/market/information")
public class MarketInformationController {
    private final MarketInformationAppService appService;
    public MarketInformationController(MarketInformationAppService appService) { this.appService = appService; }
    @GetMapping
    public Result<MarketInformationDTO.FeedDTO> feed(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String impactIndustryCode) {
        return Result.success(appService.feed(page, pageSize == null ? limit : pageSize, impactIndustryCode));
    }

    @GetMapping("/brief/latest")
    public Result<MarketInformationDTO.BriefDTO> latestBrief() {
        return Result.success(appService.latestBrief());
    }

    @GetMapping("/events/{clusterId}")
    public Result<MarketInformationDTO.EventDetailDTO> eventDetail(@PathVariable long clusterId) {
        return Result.success(appService.eventDetail(clusterId));
    }

    @PostMapping("/events/{clusterId}/analyze")
    public Result<MarketInformationDTO.AnalysisRequestDTO> analyze(
            @PathVariable long clusterId,
            @RequestParam(defaultValue = "false") boolean force) {
        return Result.success(MarketInformationDTO.AnalysisRequestDTO.builder()
                .clusterId(clusterId).queued(appService.requestAnalysis(clusterId, force)).build());
    }

    @GetMapping("/events/status")
    public Result<List<MarketInformationDTO.AnalysisStatusDTO>> statuses(@RequestParam List<Long> clusterIds) {
        return Result.success(appService.analysisStatuses(clusterIds));
    }

    @PostMapping("/pipeline/refresh")
    public Result<MarketInformationDTO.PipelineRefreshDTO> refreshPipeline() {
        return Result.success(MarketInformationDTO.PipelineRefreshDTO.builder()
                .clusterCount(appService.refreshPipeline()).build());
    }
}
