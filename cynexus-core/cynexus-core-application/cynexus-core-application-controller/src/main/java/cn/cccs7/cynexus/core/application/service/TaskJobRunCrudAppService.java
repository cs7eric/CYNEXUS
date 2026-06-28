package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.convert.TaskJobRunDTOConverter;
import cn.cccs7.cynexus.core.api.dto.TaskJobRunDTO;
import cn.cccs7.cynexus.core.domain.entity.TaskJobRunBO;
import cn.cccs7.cynexus.core.domain.service.TaskJobRunDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskJobRunCrudAppService {

    private final TaskJobRunDomainService taskJobRunDomainService;

    public TaskJobRunCrudAppService(TaskJobRunDomainService taskJobRunDomainService) {
        this.taskJobRunDomainService = taskJobRunDomainService;
    }

    public List<TaskJobRunDTO> list() {
        return TaskJobRunDTOConverter.INSTANCE.toSourceList(taskJobRunDomainService.list());
    }

    public TaskJobRunDTO findById(Long id) {
        TaskJobRunBO value = taskJobRunDomainService.findById(id);
        return value == null ? null : TaskJobRunDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(TaskJobRunDTO dto) {
        return taskJobRunDomainService.create(TaskJobRunDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(TaskJobRunDTO dto) {
        return taskJobRunDomainService.update(TaskJobRunDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return taskJobRunDomainService.delete(id);
    }
}
