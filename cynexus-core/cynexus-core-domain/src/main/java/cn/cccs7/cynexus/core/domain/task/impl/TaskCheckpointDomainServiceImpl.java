package cn.cccs7.cynexus.core.domain.task.impl;

import cn.cccs7.cynexus.core.domain.entity.TaskCheckpointBO;
import cn.cccs7.cynexus.core.domain.repository.TaskCheckpointRepository;
import cn.cccs7.cynexus.core.domain.task.TaskCheckpointDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskCheckpointDomainServiceImpl implements TaskCheckpointDomainService {

    private final TaskCheckpointRepository taskCheckpointRepository;

    public TaskCheckpointDomainServiceImpl(TaskCheckpointRepository taskCheckpointRepository) {
        this.taskCheckpointRepository = taskCheckpointRepository;
    }

    @Override
    public List<TaskCheckpointBO> list() {
        return taskCheckpointRepository.list();
    }

    @Override
    public TaskCheckpointBO findById(Long id) {
        return taskCheckpointRepository.findById(id);
    }

    @Override
    public Long create(TaskCheckpointBO value) {
        return taskCheckpointRepository.create(value);
    }

    @Override
    public boolean update(TaskCheckpointBO value) {
        return taskCheckpointRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return taskCheckpointRepository.delete(id);
    }
}
