package cn.cccs7.cynexus.wechat.domain.model;

public record WechatVisionResult(
        String executionNo,
        String status,
        String modelCode,
        String content) {
}
