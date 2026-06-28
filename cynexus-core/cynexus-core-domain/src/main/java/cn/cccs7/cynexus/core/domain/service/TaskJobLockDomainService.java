package cn.cccs7.cynexus.core.domain.service;

import cn.cccs7.cynexus.core.domain.entity.TaskJobLockBO;
import java.util.List;

public interface TaskJobLockDomainService {

    List<TaskJobLockBO> list();

    TaskJobLockBO findById(Long id);

    Long create(TaskJobLockBO value);

    boolean update(TaskJobLockBO value);

    boolean delete(Long id);
}
