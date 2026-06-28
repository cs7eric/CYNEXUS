package cn.cccs7.cynexus.core.domain.repository;

import cn.cccs7.cynexus.core.domain.entity.TaskJobLockBO;
import java.util.List;

public interface TaskJobLockRepository {

    List<TaskJobLockBO> list();

    TaskJobLockBO findById(Long id);

    Long create(TaskJobLockBO value);

    boolean update(TaskJobLockBO value);

    boolean delete(Long id);
}
