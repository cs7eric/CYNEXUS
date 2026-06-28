package cn.cccs7.cynexus.core.domain.repository;

import cn.cccs7.cynexus.core.domain.entity.TaskJobRunBO;
import java.util.List;

public interface TaskJobRunRepository {

    List<TaskJobRunBO> list();

    TaskJobRunBO findById(Long id);

    Long create(TaskJobRunBO value);

    boolean update(TaskJobRunBO value);

    boolean delete(Long id);
}
