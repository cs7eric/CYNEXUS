package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.MediaTranscriptPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.MediaTranscriptMapper;
import cn.cccs7.cynexus.core.infra.basic.service.MediaTranscriptBasicService;
import org.springframework.stereotype.Service;

@Service
public class MediaTranscriptBasicServiceImpl extends AbstractCrudBasicService<MediaTranscriptPO>
        implements MediaTranscriptBasicService {

    public MediaTranscriptBasicServiceImpl(MediaTranscriptMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
