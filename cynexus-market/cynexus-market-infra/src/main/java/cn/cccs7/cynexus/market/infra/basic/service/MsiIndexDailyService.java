package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.market.infra.basic.entity.MsiIndexDaily;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * (MsiIndexDaily)表服务接口
 *
 * @author cccs7 - csq020611@gmail.com
 * @since 2026-06-27 15:03:09
 */
public interface MsiIndexDailyService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    MsiIndexDaily queryById(Long id);



    /**
     * 新增数据
     *
     * @param msiIndexDaily 实例对象
     * @return 实例对象
     */
    MsiIndexDaily insert(MsiIndexDaily msiIndexDaily);

    /**
     * 修改数据
     *
     * @param msiIndexDaily 实例对象
     * @return 实例对象
     */
    MsiIndexDaily update(MsiIndexDaily msiIndexDaily);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
