package cn.cccs7.cynexus.core.domain.service.impl;

import cn.cccs7.cynexus.core.domain.entity.TaskJobRunBO;
import cn.cccs7.cynexus.core.domain.repository.TaskJobRunRepository;
import cn.cccs7.cynexus.core.domain.service.TaskJobRunDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskJobRunDomainServiceImpl implements TaskJobRunDomainService {

    private final TaskJobRunRepository taskJobRunRepository;

    public TaskJobRunDomainServiceImpl(TaskJobRunRepository taskJobRunRepository) {
        this.taskJobRunRepository = taskJobRunRepository;
    }

    @Override
    public List<TaskJobRunBO> list() {
        return taskJobRunRepository.list();
    }

    @Override
    public TaskJobRunBO findById(Long id) {
        return taskJobRunRepository.findById(id);
    }

    @Override
    public Long create(TaskJobRunBO value) {
        return taskJobRunRepository.create(value);
    }

    @Override
    public boolean update(TaskJobRunBO value) {
        return taskJobRunRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return taskJobRunRepository.delete(id);
    }
}
