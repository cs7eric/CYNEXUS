package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "media_transcript", autoResultMap = true)
public class MediaTranscriptPO extends BasePO {

    private String transcriptId;
    private String sourceUri;
    private String transcript;
    private String language;
    private BigDecimal durationSeconds;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadata;
}
