package cn.cccs7.cynexus.core.domain.service;

import cn.cccs7.cynexus.core.domain.entity.TaskCheckpointBO;
import java.util.List;

public interface TaskCheckpointDomainService {

    List<TaskCheckpointBO> list();

    TaskCheckpointBO findById(Long id);

    Long create(TaskCheckpointBO value);

    boolean update(TaskCheckpointBO value);

    boolean delete(Long id);
}
