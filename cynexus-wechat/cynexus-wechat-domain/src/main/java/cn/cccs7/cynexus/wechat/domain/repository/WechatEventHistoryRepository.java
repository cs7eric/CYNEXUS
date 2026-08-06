package cn.cccs7.cynexus.wechat.domain.repository;

import cn.cccs7.cynexus.wechat.domain.model.WechatEventHistory;
import cn.cccs7.cynexus.wechat.domain.model.WechatEventHistoryPage;
import cn.cccs7.cynexus.wechat.domain.model.WechatImageRoute;
import cn.cccs7.cynexus.wechat.domain.model.WechatStoredImage;

public interface WechatEventHistoryRepository {
    long findActiveIdByMessageId(String messageId);

    long create(WechatEventHistory event);

    boolean attachImage(long id, WechatStoredImage image);

    boolean markFailed(long id, String errorMessage);

    WechatEventHistory claimNextImage();

    boolean completeRouting(long id, WechatImageRoute route);

    boolean failRouting(long id, String errorMessage);

    WechatEventHistoryPage list(int limit, int offset, String messageType, String processingStatus);
}
