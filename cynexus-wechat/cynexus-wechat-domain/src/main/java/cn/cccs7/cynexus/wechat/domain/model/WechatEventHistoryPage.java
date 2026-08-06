package cn.cccs7.cynexus.wechat.domain.model;

import java.util.List;

public record WechatEventHistoryPage(List<WechatEventHistory> items, long total, int limit, int offset) {
}
