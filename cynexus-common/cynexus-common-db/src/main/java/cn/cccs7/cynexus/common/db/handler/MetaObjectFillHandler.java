package cn.cccs7.cynexus.common.db.handler;

import cn.cccs7.cynexus.common.core.constant.OperatorConstants;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.OffsetDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

@Component
public class MetaObjectFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createdBy", String.class, OperatorConstants.ACTOR);
        strictInsertFill(metaObject, "updatedBy", String.class, OperatorConstants.ACTOR);
        strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, OffsetDateTime.now());
        strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now());
        strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedBy", String.class, OperatorConstants.ACTOR);
        strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now());
    }
}
