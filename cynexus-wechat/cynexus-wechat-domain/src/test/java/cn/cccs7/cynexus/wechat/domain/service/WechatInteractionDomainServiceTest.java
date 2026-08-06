package cn.cccs7.cynexus.wechat.domain.service;

import cn.cccs7.cynexus.wechat.domain.model.WechatInboundMessage;
import cn.cccs7.cynexus.wechat.domain.model.WechatReply;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatInteractionDomainServiceTest {
    private final WechatInteractionDomainService service = new WechatInteractionDomainService();

    @Test
    void verifiesOfficialAccountCallbackSignature() throws Exception {
        String[] values = {"token", "1710000000", "nonce"};
        Arrays.sort(values);
        String signature = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                .digest(String.join("", values).getBytes(StandardCharsets.UTF_8)));

        assertTrue(service.verifySignature("token", signature, "1710000000", "nonce"));
        assertFalse(service.verifySignature("token", "invalid", "1710000000", "nonce"));
    }

    @Test
    void createsRepliesForSubscribeAndTextMessages() {
        WechatReply subscribe = service.reply(
                new WechatInboundMessage("event", "subscribe", "original", "subscriber", null),
                "welcome", "default");
        WechatReply text = service.reply(
                new WechatInboundMessage("text", null, "original", "subscriber", "hello"),
                "welcome", "default");

        assertNotNull(subscribe);
        assertTrue(subscribe.toXml().contains("welcome"));
        assertNotNull(text);
        assertTrue(text.toXml().contains("default"));

        WechatReply image = service.reply(
                new WechatInboundMessage("image", null, "original", "subscriber", null,
                        "media-123", "https://example.test/image.jpg"),
                "welcome", "default", "image received");
        assertNotNull(image);
        assertTrue(image.toXml().contains("image received"));
    }
}
