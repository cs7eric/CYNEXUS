package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.entity.TaskCheckpointBO;
import cn.cccs7.cynexus.core.domain.repository.TaskCheckpointRepository;
import cn.cccs7.cynexus.core.infra.basic.service.TaskCheckpointBasicService;
import cn.cccs7.cynexus.core.infra.convert.TaskCheckpointPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TaskCheckpointRepositoryImpl implements TaskCheckpointRepository {

    private final TaskCheckpointBasicService taskCheckpointBasicService;

    public TaskCheckpointRepositoryImpl(TaskCheckpointBasicService taskCheckpointBasicService) {
        this.taskCheckpointBasicService = taskCheckpointBasicService;
    }

    @Override
    public List<TaskCheckpointBO> list() {
        return TaskCheckpointPOConverter.INSTANCE.toTargetList(taskCheckpointBasicService.listAll());
    }

    @Override
    public TaskCheckpointBO findById(Long id) {
        return Optional.ofNullable(taskCheckpointBasicService.getById(id))
                .map(TaskCheckpointPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(TaskCheckpointBO value) {
        return taskCheckpointBasicService.create(TaskCheckpointPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(TaskCheckpointBO value) {
        return taskCheckpointBasicService.update(TaskCheckpointPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return taskCheckpointBasicService.delete(id);
    }
}
