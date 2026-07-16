package cn.cccs7.cynexus.core.domain.task.impl;

import cn.cccs7.cynexus.core.domain.entity.TaskJobLockBO;
import cn.cccs7.cynexus.core.domain.repository.TaskJobLockRepository;
import cn.cccs7.cynexus.core.domain.task.TaskJobLockDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskJobLockDomainServiceImpl implements TaskJobLockDomainService {

    private final TaskJobLockRepository taskJobLockRepository;

    public TaskJobLockDomainServiceImpl(TaskJobLockRepository taskJobLockRepository) {
        this.taskJobLockRepository = taskJobLockRepository;
    }

    @Override
    public List<TaskJobLockBO> list() {
        return taskJobLockRepository.list();
    }

    @Override
    public TaskJobLockBO findById(Long id) {
        return taskJobLockRepository.findById(id);
    }

    @Override
    public Long create(TaskJobLockBO value) {
        return taskJobLockRepository.create(value);
    }

    @Override
    public boolean update(TaskJobLockBO value) {
        return taskJobLockRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return taskJobLockRepository.delete(id);
    }
}
