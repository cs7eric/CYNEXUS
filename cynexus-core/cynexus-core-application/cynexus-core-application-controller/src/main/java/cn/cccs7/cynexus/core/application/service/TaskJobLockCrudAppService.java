package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.convert.TaskJobLockDTOConverter;
import cn.cccs7.cynexus.core.api.dto.TaskJobLockDTO;
import cn.cccs7.cynexus.core.domain.entity.TaskJobLockBO;
import cn.cccs7.cynexus.core.domain.service.TaskJobLockDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskJobLockCrudAppService {

    private final TaskJobLockDomainService taskJobLockDomainService;

    public TaskJobLockCrudAppService(TaskJobLockDomainService taskJobLockDomainService) {
        this.taskJobLockDomainService = taskJobLockDomainService;
    }

    public List<TaskJobLockDTO> list() {
        return TaskJobLockDTOConverter.INSTANCE.toSourceList(taskJobLockDomainService.list());
    }

    public TaskJobLockDTO findById(Long id) {
        TaskJobLockBO value = taskJobLockDomainService.findById(id);
        return value == null ? null : TaskJobLockDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(TaskJobLockDTO dto) {
        return taskJobLockDomainService.create(TaskJobLockDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(TaskJobLockDTO dto) {
        return taskJobLockDomainService.update(TaskJobLockDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return taskJobLockDomainService.delete(id);
    }
}
