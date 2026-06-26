package cn.cccs7.cynexus.common.db.service;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import java.util.List;

public interface CrudBasicService<T extends BasePO> {

    List<T> listAll();

    T getById(Long id);

    Long create(T po);

    boolean update(T po);

    boolean delete(Long id);
}
