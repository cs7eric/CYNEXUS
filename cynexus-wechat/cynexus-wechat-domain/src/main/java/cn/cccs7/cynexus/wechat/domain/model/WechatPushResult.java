package cn.cccs7.cynexus.wechat.domain.model;

import java.util.List;

public record WechatPushResult(int requested, int sent, List<WechatPushFailure> failures) {
}
