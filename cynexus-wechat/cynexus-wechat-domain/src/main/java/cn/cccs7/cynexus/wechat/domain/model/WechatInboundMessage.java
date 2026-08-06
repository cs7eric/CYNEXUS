package cn.cccs7.cynexus.wechat.domain.model;

public record WechatInboundMessage(
        String messageType,
        String event,
        String toUserName,
        String fromUserName,
        String content,
        String mediaId,
        String picUrl,
        String messageId) {

    public WechatInboundMessage(String messageType,
                                String event,
                                String toUserName,
                                String fromUserName,
                                String content) {
        this(messageType, event, toUserName, fromUserName, content, null, null, null);
    }

    public WechatInboundMessage(String messageType,
                                String event,
                                String toUserName,
                                String fromUserName,
                                String content,
                                String mediaId,
                                String picUrl) {
        this(messageType, event, toUserName, fromUserName, content, mediaId, picUrl, null);
    }

    public boolean isEvent(String expectedEvent) {
        return "event".equals(messageType) && expectedEvent.equalsIgnoreCase(event);
    }

    public boolean isText() {
        return "text".equalsIgnoreCase(messageType);
    }

    public boolean isImage() {
        return "image".equalsIgnoreCase(messageType);
    }
}
