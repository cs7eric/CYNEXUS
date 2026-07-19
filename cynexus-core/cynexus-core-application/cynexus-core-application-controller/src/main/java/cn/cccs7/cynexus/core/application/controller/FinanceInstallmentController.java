package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceCreditInstallmentDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceCreditInstallmentPaymentRequest;
import cn.cccs7.cynexus.core.application.request.finance.FinanceCreditInstallmentPlanRequest;
import cn.cccs7.cynexus.core.application.service.FinanceInstallmentAppService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/finance/installments")
public class FinanceInstallmentController {
    private final FinanceInstallmentAppService service;

    public FinanceInstallmentController(FinanceInstallmentAppService service) { this.service = service; }

    @GetMapping("/plans")
    public Result<List<FinanceCreditInstallmentDTO.Plan>> list(@RequestParam(required = false) Long accountId) {
        return Result.success(service.list(accountId));
    }

    @PostMapping("/plans")
    public Result<FinanceCreditInstallmentDTO.Plan> create(@RequestBody FinanceCreditInstallmentPlanRequest request) {
        return Result.success(service.create(request));
    }

    @PostMapping("/plans/{planId}/installments/{installmentNo}/pay")
    public Result<FinanceCreditInstallmentDTO.Plan> pay(@PathVariable Long planId, @PathVariable Integer installmentNo,
                                                        @RequestBody FinanceCreditInstallmentPaymentRequest request) {
        return Result.success(service.pay(planId, installmentNo, request));
    }
}
