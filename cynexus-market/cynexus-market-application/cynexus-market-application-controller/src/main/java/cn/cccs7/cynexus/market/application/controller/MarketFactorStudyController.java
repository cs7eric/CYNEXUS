package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketFactorStudyDTO;
import cn.cccs7.cynexus.market.application.service.MarketFactorStudyAppService;
import cn.cccs7.cynexus.market.application.service.stockselection.StockSelectionIdentityResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/market/factor-studies", "/api/market/screener/factor-research/studies"})
public class MarketFactorStudyController {
    private final MarketFactorStudyAppService appService;
    private final StockSelectionIdentityResolver identityResolver;

    public MarketFactorStudyController(MarketFactorStudyAppService appService, StockSelectionIdentityResolver identityResolver) {
        this.appService = appService; this.identityResolver = identityResolver;
    }

    @PostMapping
    public ResponseEntity<Result<MarketFactorStudyDTO.Response>> create(@RequestBody MarketFactorStudyDTO.CreateRequest request,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.success(appService.queue(identityResolver.resolve(principal, tenant, signature), request)));
    }

    @GetMapping
    public Result<java.util.List<MarketFactorStudyDTO.Response>> list(@RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.list(identityResolver.resolve(principal, tenant, signature), offset, limit));
    }

    @GetMapping("/{studyId}")
    public Result<MarketFactorStudyDTO.Response> get(@PathVariable String studyId,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.find(identityResolver.resolve(principal, tenant, signature), studyId));
    }

    @GetMapping("/{studyId}/samples")
    public Result<java.util.List<MarketFactorStudyDTO.SampleResponse>> samples(@PathVariable String studyId,
            @RequestParam(required = false) String sampleType, @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.samples(identityResolver.resolve(principal, tenant, signature), studyId, sampleType, offset, limit));
    }

    @GetMapping("/{studyId}/sequences")
    public Result<java.util.List<MarketFactorStudyDTO.SequencePointResponse>> sequences(@PathVariable String studyId,
            @RequestParam(required = false) String sequenceCode, @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.sequences(identityResolver.resolve(principal, tenant, signature), studyId, sequenceCode, offset, limit));
    }

    @GetMapping("/{studyId}/discoveries")
    public Result<java.util.List<MarketFactorStudyDTO.FactorResultResponse>> discoveries(@PathVariable String studyId,
            @RequestParam(required = false) Integer offset, @RequestParam(required = false) Integer limit,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.discoveries(identityResolver.resolve(principal, tenant, signature), studyId, offset, limit));
    }

    @GetMapping("/{studyId}/clusters")
    public Result<java.util.List<MarketFactorStudyDTO.ClusterResponse>> clusters(@PathVariable String studyId,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.clusters(identityResolver.resolve(principal, tenant, signature), studyId));
    }

    @GetMapping("/{studyId}/discoveries/{discoveryCode}")
    public Result<MarketFactorStudyDTO.FactorResultResponse> discovery(@PathVariable String studyId, @PathVariable String discoveryCode,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.discovery(identityResolver.resolve(principal, tenant, signature), studyId, discoveryCode));
    }

    @PostMapping("/{studyId}/discoveries/{discoveryCode}/promote")
    public Result<MarketFactorStudyDTO.FactorResultResponse> promote(@PathVariable String studyId, @PathVariable String discoveryCode,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.promote(identityResolver.resolve(principal, tenant, signature), studyId, discoveryCode));
    }

    @GetMapping("/{studyId}/outcomes")
    public Result<java.util.List<java.util.Map<String, Object>>> outcomes(@PathVariable String studyId,
            @RequestParam(required = false) Integer offset, @RequestParam(required = false) Integer limit,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.outcomes(identityResolver.resolve(principal, tenant, signature), studyId, offset, limit));
    }

    @PostMapping("/{studyId}/cancel")
    public Result<MarketFactorStudyDTO.Response> cancel(@PathVariable String studyId,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.cancel(identityResolver.resolve(principal, tenant, signature), studyId));
    }

    @PostMapping("/{studyId}/retry")
    public ResponseEntity<Result<MarketFactorStudyDTO.Response>> retry(@PathVariable String studyId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.success(
                appService.retry(identityResolver.resolve(principal, tenant, signature), studyId, idempotencyKey)));
    }
}
