package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceAccountDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceAccountRequest;
import cn.cccs7.cynexus.core.application.service.FinanceLedgerAppService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/finance/accounts")
public class FinanceAccountController {
    private final FinanceLedgerAppService service;

    public FinanceAccountController(FinanceLedgerAppService service) { this.service = service; }

    @GetMapping
    public Result<List<FinanceAccountDTO>> list(@RequestParam(defaultValue = "false") boolean includeArchived) { return Result.success(service.listAccounts(includeArchived)); }

    @PostMapping
    public Result<FinanceAccountDTO> save(@RequestBody FinanceAccountRequest request) { return Result.success(service.saveAccount(request)); }

    @DeleteMapping("/{id}")
    public Result<Boolean> archive(@org.springframework.web.bind.annotation.PathVariable Long id) { return Result.success(service.archiveAccount(id)); }

    @PostMapping("/{id}/restore")
    public Result<Boolean> restore(@org.springframework.web.bind.annotation.PathVariable Long id) { return Result.success(service.restoreAccount(id)); }
}
