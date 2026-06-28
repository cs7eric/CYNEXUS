package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.ThsIndustryConstituentSnapshotDTO;
import cn.cccs7.cynexus.market.application.service.ThsIndustryConstituentSnapshotCrudAppService;
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
@RequestMapping("/api/market/ths-industry-constituent-snapshot")
public class ThsIndustryConstituentSnapshotCrudController {

    private final ThsIndustryConstituentSnapshotCrudAppService appService;

    public ThsIndustryConstituentSnapshotCrudController(ThsIndustryConstituentSnapshotCrudAppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public Result<List<ThsIndustryConstituentSnapshotDTO>> list() {
        return Result.success(appService.list());
    }

    @GetMapping("/{id}")
    public Result<ThsIndustryConstituentSnapshotDTO> get(@PathVariable("id") Long id) {
        return Result.success(appService.findById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody ThsIndustryConstituentSnapshotDTO dto) {
        return Result.success(appService.create(dto));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody ThsIndustryConstituentSnapshotDTO dto) {
        return Result.success(appService.update(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        return Result.success(appService.delete(id));
    }
}
