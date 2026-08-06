package cn.cccs7.cynexus.wechat.application.controller;

import cn.cccs7.cynexus.wechat.application.service.WechatCallbackAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wechat")
public class WechatCallbackController {
    private final WechatCallbackAppService appService;

    public WechatCallbackController(WechatCallbackAppService appService) {
        this.appService = appService;
    }

    @GetMapping("/callback")
    public String verify(@RequestParam String signature,
                         @RequestParam String timestamp,
                         @RequestParam String nonce,
                         @RequestParam String echostr) {
        return appService.verify(signature, timestamp, nonce, echostr);
    }

    @PostMapping(value = "/callback", consumes = "text/xml")
    public String callback(@RequestBody String body,
                           @RequestParam String signature,
                           @RequestParam String timestamp,
                           @RequestParam String nonce) {
        return appService.handle(body, signature, timestamp, nonce);
    }
}
