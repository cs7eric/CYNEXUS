package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.market.infra.basic.entity.MsiLimitEmotionDaily;
import cn.cccs7.cynexus.market.infra.basic.dao.MsiLimitEmotionDailyDao;
import cn.cccs7.cynexus.market.infra.basic.service.MsiLimitEmotionDailyService;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import javax.annotation.Resource;

/**
 * (MsiLimitEmotionDaily)表服务实现类
 *
 * @author cccs7 - csq020611@gmail.com
 * @since 2026-06-27 15:05:21
 */
@Service("msiLimitEmotionDailyService")
public class MsiLimitEmotionDailyServiceImpl implements MsiLimitEmotionDailyService {
    @Resource
    private MsiLimitEmotionDailyDao msiLimitEmotionDailyDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public MsiLimitEmotionDaily queryById(Long id) {
        return this.msiLimitEmotionDailyDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param msiLimitEmotionDaily 筛选条件
     * @param pageRequest      分页对象
     * @return 查询结果
     */
    @Override
    public Page<MsiLimitEmotionDaily> queryByPage(MsiLimitEmotionDaily msiLimitEmotionDaily, PageRequest pageRequest) {
        long total = this.msiLimitEmotionDailyDao.count(msiLimitEmotionDaily);
        return new PageImpl<>(this.msiLimitEmotionDailyDao.queryAllByLimit(msiLimitEmotionDaily, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param msiLimitEmotionDaily 实例对象
     * @return 实例对象
     */
    @Override
    public MsiLimitEmotionDaily insert(MsiLimitEmotionDaily msiLimitEmotionDaily) {
        this.msiLimitEmotionDailyDao.insert(msiLimitEmotionDaily);
        return msiLimitEmotionDaily;
    }

    /**
     * 修改数据
     *
     * @param msiLimitEmotionDaily 实例对象
     * @return 实例对象
     */
    @Override
    public MsiLimitEmotionDaily update(MsiLimitEmotionDaily msiLimitEmotionDaily) {
        this.msiLimitEmotionDailyDao.update(msiLimitEmotionDaily);
        return this.queryById(msiLimitEmotionDaily.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.msiLimitEmotionDailyDao.deleteById(id) > 0;
    }
}
