package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "media_transcription_history", autoResultMap = true)
public class MediaTranscriptionHistoryPO extends BasePO {

    private String taskId;
    private String title;
    private String status;
    private String fileName;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String recordJson;
    private Integer sortOrder;
}
