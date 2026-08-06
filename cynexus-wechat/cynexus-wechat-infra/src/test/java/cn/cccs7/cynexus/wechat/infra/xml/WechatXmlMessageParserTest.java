package cn.cccs7.cynexus.wechat.infra.xml;

import cn.cccs7.cynexus.wechat.domain.model.WechatInboundMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WechatXmlMessageParserTest {
    @Test
    void parsesOfficialAccountTextMessage() {
        String xml = "<xml>"
                + "<ToUserName><![CDATA[original]]></ToUserName>"
                + "<FromUserName><![CDATA[subscriber]]></FromUserName>"
                + "<MsgType><![CDATA[text]]></MsgType>"
                + "<Content><![CDATA[hello]]></Content>"
                + "</xml>";

        WechatInboundMessage message = new WechatXmlMessageParser().parse(xml);

        assertEquals("text", message.messageType());
        assertEquals("original", message.toUserName());
        assertEquals("subscriber", message.fromUserName());
        assertEquals("hello", message.content());
    }

    @Test
    void parsesOfficialAccountImageMessageMetadata() {
        String xml = "<xml>"
                + "<ToUserName><![CDATA[original]]></ToUserName>"
                + "<FromUserName><![CDATA[subscriber]]></FromUserName>"
                + "<MsgType><![CDATA[image]]></MsgType>"
                + "<MediaId><![CDATA[media-123]]></MediaId>"
                + "<PicUrl><![CDATA[https://example.test/image.jpg]]></PicUrl>"
                + "</xml>";

        WechatInboundMessage message = new WechatXmlMessageParser().parse(xml);

        assertEquals("image", message.messageType());
        assertEquals("media-123", message.mediaId());
        assertEquals("https://example.test/image.jpg", message.picUrl());
    }
}
