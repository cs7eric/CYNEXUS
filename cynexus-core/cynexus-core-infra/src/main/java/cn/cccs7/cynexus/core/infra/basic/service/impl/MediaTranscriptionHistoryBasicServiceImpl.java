package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.MediaTranscriptionHistoryPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.MediaTranscriptionHistoryMapper;
import cn.cccs7.cynexus.core.infra.basic.service.MediaTranscriptionHistoryBasicService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MediaTranscriptionHistoryBasicServiceImpl
        extends AbstractCrudBasicService<MediaTranscriptionHistoryPO>
        implements MediaTranscriptionHistoryBasicService {

    private final MediaTranscriptionHistoryMapper mapper;

    public MediaTranscriptionHistoryBasicServiceImpl(MediaTranscriptionHistoryMapper mapper,
                                                    SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public List<MediaTranscriptionHistoryPO> listActive() {
        return mapper.selectActive();
    }

    @Override
    public MediaTranscriptionHistoryPO getActiveById(Long id) {
        return mapper.selectActiveById(id);
    }

    @Override
    public MediaTranscriptionHistoryPO getActiveByTaskId(String taskId) {
        return mapper.selectActiveByTaskId(taskId);
    }

    @Override
    public boolean updateSortOrder(Long id, Integer sortOrder) {
        return mapper.updateSortOrder(id, sortOrder) > 0;
    }

    @Override
    public boolean softDelete(Long id) {
        return mapper.softDelete(id) > 0;
    }
}
