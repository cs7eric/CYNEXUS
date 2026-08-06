package cn.cccs7.cynexus.wechat.api.dto;

import java.util.List;

public record WechatPushResultDTO(int requested, int sent, List<WechatPushFailureDTO> failures) {
}
