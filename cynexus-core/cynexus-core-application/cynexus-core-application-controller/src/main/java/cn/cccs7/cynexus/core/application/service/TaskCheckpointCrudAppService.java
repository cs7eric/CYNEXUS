package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.convert.TaskCheckpointDTOConverter;
import cn.cccs7.cynexus.core.api.dto.TaskCheckpointDTO;
import cn.cccs7.cynexus.core.domain.entity.TaskCheckpointBO;
import cn.cccs7.cynexus.core.domain.task.TaskCheckpointDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskCheckpointCrudAppService {

    private final TaskCheckpointDomainService taskCheckpointDomainService;

    public TaskCheckpointCrudAppService(TaskCheckpointDomainService taskCheckpointDomainService) {
        this.taskCheckpointDomainService = taskCheckpointDomainService;
    }

    public List<TaskCheckpointDTO> list() {
        return TaskCheckpointDTOConverter.INSTANCE.toSourceList(taskCheckpointDomainService.list());
    }

    public TaskCheckpointDTO findById(Long id) {
        TaskCheckpointBO value = taskCheckpointDomainService.findById(id);
        return value == null ? null : TaskCheckpointDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(TaskCheckpointDTO dto) {
        return taskCheckpointDomainService.create(TaskCheckpointDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(TaskCheckpointDTO dto) {
        return taskCheckpointDomainService.update(TaskCheckpointDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return taskCheckpointDomainService.delete(id);
    }
}
