package cn.cccs7.cynexus.wechat.domain.service;

import cn.cccs7.cynexus.wechat.domain.model.WechatInboundMessage;
import cn.cccs7.cynexus.wechat.domain.model.WechatReply;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

public class WechatInteractionDomainService {

    public boolean verifySignature(String token, String signature, String timestamp, String nonce) {
        if (isBlank(token) || isBlank(signature) || isBlank(timestamp) || isBlank(nonce)) {
            return false;
        }
        String[] values = {token, timestamp, nonce};
        Arrays.sort(values);
        String expected = sha1(String.join("", values));
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII));
    }

    public WechatReply reply(WechatInboundMessage message, String welcomeReply, String defaultReply) {
        return reply(message, welcomeReply, defaultReply, defaultReply);
    }

    public WechatReply reply(WechatInboundMessage message,
                             String welcomeReply,
                             String defaultReply,
                             String imageReply) {
        if (message == null || isBlank(message.fromUserName()) || isBlank(message.toUserName())) {
            return null;
        }
        String content = null;
        if (message.isEvent("subscribe")) {
            content = welcomeReply;
        } else if (message.isImage()) {
            content = imageReply;
        } else if (message.isText()) {
            content = defaultReply;
        }
        return isBlank(content) ? null : new WechatReply(message.fromUserName(), message.toUserName(), content);
    }

    private static String sha1(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate WeChat callback signature", exception);
        }
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
