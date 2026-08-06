package cn.cccs7.cynexus.wechat.domain.model;

public record WechatStoredImage(
        String mediaId,
        String sourceUrl,
        String storagePath,
        String contentType,
        long size) {
}
