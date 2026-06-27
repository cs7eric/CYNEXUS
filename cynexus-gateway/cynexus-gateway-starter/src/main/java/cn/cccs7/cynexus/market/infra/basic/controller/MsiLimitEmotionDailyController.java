package cn.cccs7.cynexus.market.infra.basic.controller;

import cn.cccs7.cynexus.market.infra.basic.entity.MsiLimitEmotionDaily;
import cn.cccs7.cynexus.market.infra.basic.service.MsiLimitEmotionDailyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * (MsiLimitEmotionDaily)表控制层
 *
 * @author cccs7 - csq020611@gmail.com
 * @since 2026-06-27 15:05:21
 */
@RestController
@RequestMapping("msiLimitEmotionDaily")
public class MsiLimitEmotionDailyController {
    /**
     * 服务对象
     */
    @Resource
    private MsiLimitEmotionDailyService msiLimitEmotionDailyService;

    /**
     * 分页查询
     *
     * @param msiLimitEmotionDaily 筛选条件
     * @param pageRequest      分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<MsiLimitEmotionDaily>> queryByPage(MsiLimitEmotionDaily msiLimitEmotionDaily, PageRequest pageRequest) {
        return ResponseEntity.ok(this.msiLimitEmotionDailyService.queryByPage(msiLimitEmotionDaily, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<MsiLimitEmotionDaily> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.msiLimitEmotionDailyService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param msiLimitEmotionDaily 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<MsiLimitEmotionDaily> add(MsiLimitEmotionDaily msiLimitEmotionDaily) {
        return ResponseEntity.ok(this.msiLimitEmotionDailyService.insert(msiLimitEmotionDaily));
    }

    /**
     * 编辑数据
     *
     * @param msiLimitEmotionDaily 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<MsiLimitEmotionDaily> edit(MsiLimitEmotionDaily msiLimitEmotionDaily) {
        return ResponseEntity.ok(this.msiLimitEmotionDailyService.update(msiLimitEmotionDaily));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.msiLimitEmotionDailyService.deleteById(id));
    }

}

