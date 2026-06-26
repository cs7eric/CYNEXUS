package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.SectorRankDTO;
import cn.cccs7.cynexus.market.application.assembler.SectorRankAssembler;
import cn.cccs7.cynexus.market.application.service.SectorRankAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/sectors")
public class SectorController {

    private final SectorRankAppService sectorRankAppService;
    private final SectorRankAssembler sectorRankAssembler;

    public SectorController(SectorRankAppService sectorRankAppService,
                            SectorRankAssembler sectorRankAssembler) {
        this.sectorRankAppService = sectorRankAppService;
        this.sectorRankAssembler = sectorRankAssembler;
    }

    @GetMapping("/rank")
    public Result<SectorRankDTO> getSectorRank(@RequestParam String tradeDate, @RequestParam Integer topN) {
        return Result.success(sectorRankAssembler.toDTO(tradeDate, topN,
                sectorRankAppService.getSectorRank(tradeDate, topN)));
    }
}
