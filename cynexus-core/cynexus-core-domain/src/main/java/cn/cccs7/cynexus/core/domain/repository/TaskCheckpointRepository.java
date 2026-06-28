package cn.cccs7.cynexus.core.domain.repository;

import cn.cccs7.cynexus.core.domain.entity.TaskCheckpointBO;
import java.util.List;

public interface TaskCheckpointRepository {

    List<TaskCheckpointBO> list();

    TaskCheckpointBO findById(Long id);

    Long create(TaskCheckpointBO value);

    boolean update(TaskCheckpointBO value);

    boolean delete(Long id);
}
