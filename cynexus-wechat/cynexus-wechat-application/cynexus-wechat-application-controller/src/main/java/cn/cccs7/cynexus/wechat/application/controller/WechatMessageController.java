package cn.cccs7.cynexus.wechat.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.wechat.api.dto.WechatPushResultDTO;
import cn.cccs7.cynexus.wechat.api.dto.WechatTextPushRequest;
import cn.cccs7.cynexus.wechat.application.service.WechatMessageAppService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wechat/messages")
public class WechatMessageController {
    private final WechatMessageAppService appService;

    public WechatMessageController(WechatMessageAppService appService) {
        this.appService = appService;
    }

    @PostMapping("/text")
    public Result<WechatPushResultDTO> pushText(@Valid @RequestBody WechatTextPushRequest request) {
        return Result.success(appService.pushText(request));
    }
}
