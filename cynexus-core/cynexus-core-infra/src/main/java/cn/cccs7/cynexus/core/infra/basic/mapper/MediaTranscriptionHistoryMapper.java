package cn.cccs7.cynexus.core.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.core.infra.basic.entity.MediaTranscriptionHistoryPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MediaTranscriptionHistoryMapper extends BaseMapperX<MediaTranscriptionHistoryPO> {

    List<MediaTranscriptionHistoryPO> selectActive();

    MediaTranscriptionHistoryPO selectActiveById(@Param("id") Long id);

    MediaTranscriptionHistoryPO selectActiveByTaskId(@Param("taskId") String taskId);

    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);

    int softDelete(@Param("id") Long id);
}
