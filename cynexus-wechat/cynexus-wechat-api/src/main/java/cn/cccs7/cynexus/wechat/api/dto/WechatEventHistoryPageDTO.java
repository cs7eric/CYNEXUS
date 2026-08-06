package cn.cccs7.cynexus.wechat.api.dto;

import java.util.List;

public record WechatEventHistoryPageDTO(
        List<WechatEventHistoryDTO> items,
        long total,
        int limit,
        int offset) {
}
