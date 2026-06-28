package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.entity.TaskJobLockBO;
import cn.cccs7.cynexus.core.domain.repository.TaskJobLockRepository;
import cn.cccs7.cynexus.core.infra.basic.service.TaskJobLockBasicService;
import cn.cccs7.cynexus.core.infra.convert.TaskJobLockPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TaskJobLockRepositoryImpl implements TaskJobLockRepository {

    private final TaskJobLockBasicService taskJobLockBasicService;

    public TaskJobLockRepositoryImpl(TaskJobLockBasicService taskJobLockBasicService) {
        this.taskJobLockBasicService = taskJobLockBasicService;
    }

    @Override
    public List<TaskJobLockBO> list() {
        return TaskJobLockPOConverter.INSTANCE.toTargetList(taskJobLockBasicService.listAll());
    }

    @Override
    public TaskJobLockBO findById(Long id) {
        return Optional.ofNullable(taskJobLockBasicService.getById(id))
                .map(TaskJobLockPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(TaskJobLockBO value) {
        return taskJobLockBasicService.create(TaskJobLockPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(TaskJobLockBO value) {
        return taskJobLockBasicService.update(TaskJobLockPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return taskJobLockBasicService.delete(id);
    }
}
