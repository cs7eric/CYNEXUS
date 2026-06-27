package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.market.infra.basic.entity.MsiLimitEmotionDaily;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * (MsiLimitEmotionDaily)表服务接口
 *
 * @author cccs7 - csq020611@gmail.com
 * @since 2026-06-27 15:05:21
 */
public interface MsiLimitEmotionDailyService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    MsiLimitEmotionDaily queryById(Long id);

    /**
     * 分页查询
     *
     * @param msiLimitEmotionDaily 筛选条件
     * @param pageRequest      分页对象
     * @return 查询结果
     */
    Page<MsiLimitEmotionDaily> queryByPage(MsiLimitEmotionDaily msiLimitEmotionDaily, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param msiLimitEmotionDaily 实例对象
     * @return 实例对象
     */
    MsiLimitEmotionDaily insert(MsiLimitEmotionDaily msiLimitEmotionDaily);

    /**
     * 修改数据
     *
     * @param msiLimitEmotionDaily 实例对象
     * @return 实例对象
     */
    MsiLimitEmotionDaily update(MsiLimitEmotionDaily msiLimitEmotionDaily);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
