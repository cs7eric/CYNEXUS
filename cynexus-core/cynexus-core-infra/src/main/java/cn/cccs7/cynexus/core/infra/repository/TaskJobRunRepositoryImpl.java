package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.entity.TaskJobRunBO;
import cn.cccs7.cynexus.core.domain.repository.TaskJobRunRepository;
import cn.cccs7.cynexus.core.infra.basic.service.TaskJobRunBasicService;
import cn.cccs7.cynexus.core.infra.convert.TaskJobRunPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TaskJobRunRepositoryImpl implements TaskJobRunRepository {

    private final TaskJobRunBasicService taskJobRunBasicService;

    public TaskJobRunRepositoryImpl(TaskJobRunBasicService taskJobRunBasicService) {
        this.taskJobRunBasicService = taskJobRunBasicService;
    }

    @Override
    public List<TaskJobRunBO> list() {
        return TaskJobRunPOConverter.INSTANCE.toTargetList(taskJobRunBasicService.listAll());
    }

    @Override
    public TaskJobRunBO findById(Long id) {
        return Optional.ofNullable(taskJobRunBasicService.getById(id))
                .map(TaskJobRunPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(TaskJobRunBO value) {
        return taskJobRunBasicService.create(TaskJobRunPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(TaskJobRunBO value) {
        return taskJobRunBasicService.update(TaskJobRunPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return taskJobRunBasicService.delete(id);
    }
}
