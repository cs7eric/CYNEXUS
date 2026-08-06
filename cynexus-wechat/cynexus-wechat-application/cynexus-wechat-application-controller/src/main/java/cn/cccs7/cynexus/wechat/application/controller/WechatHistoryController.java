package cn.cccs7.cynexus.wechat.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.wechat.api.dto.WechatEventHistoryPageDTO;
import cn.cccs7.cynexus.wechat.application.service.WechatHistoryAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wechat/history")
public class WechatHistoryController {
    private final WechatHistoryAppService appService;

    public WechatHistoryController(WechatHistoryAppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public Result<WechatEventHistoryPageDTO> list(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String processingStatus) {
        return Result.success(appService.list(limit, offset, messageType, processingStatus));
    }
}
