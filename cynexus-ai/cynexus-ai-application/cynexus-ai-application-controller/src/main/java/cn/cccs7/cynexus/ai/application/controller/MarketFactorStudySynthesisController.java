package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.MarketFactorStudySynthesisDTO;
import cn.cccs7.cynexus.ai.api.request.MarketFactorStudySynthesisRequest;
import cn.cccs7.cynexus.ai.application.service.MarketFactorStudySynthesisAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/ai/market-factor-studies")
public class MarketFactorStudySynthesisController {
    private final MarketFactorStudySynthesisAppService app;
    public MarketFactorStudySynthesisController(MarketFactorStudySynthesisAppService app) { this.app = app; }
    @PostMapping("/synthesis") public Result<MarketFactorStudySynthesisDTO.Response> synthesize(@RequestBody MarketFactorStudySynthesisRequest request,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal, @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        try { return Result.success(app.synthesize(request, principal, tenant, signature)); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error); }
        catch (IllegalStateException error) { return Result.fail("AI_PROVIDER_ERROR", error.getMessage()); }
    }
}
