package cn.cccs7.cynexus.core.infra.basic.service;

import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.MediaTranscriptionHistoryPO;
import java.util.List;

public interface MediaTranscriptionHistoryBasicService extends CrudBasicService<MediaTranscriptionHistoryPO> {

    List<MediaTranscriptionHistoryPO> listActive();

    MediaTranscriptionHistoryPO getActiveById(Long id);

    MediaTranscriptionHistoryPO getActiveByTaskId(String taskId);

    boolean updateSortOrder(Long id, Integer sortOrder);

    boolean softDelete(Long id);
}
