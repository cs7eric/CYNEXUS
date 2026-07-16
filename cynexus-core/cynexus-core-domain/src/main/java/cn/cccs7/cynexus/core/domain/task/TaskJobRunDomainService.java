package cn.cccs7.cynexus.core.domain.task;

import cn.cccs7.cynexus.core.domain.entity.TaskJobRunBO;
import java.util.List;

public interface TaskJobRunDomainService {

    List<TaskJobRunBO> list();

    TaskJobRunBO findById(Long id);

    Long create(TaskJobRunBO value);

    boolean update(TaskJobRunBO value);

    boolean delete(Long id);
}
