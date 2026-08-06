package cn.cccs7.cynexus.wechat.infra.xml;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.wechat.domain.gateway.WechatInboundMessageParser;
import cn.cccs7.cynexus.wechat.domain.model.WechatInboundMessage;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@Component
public class WechatXmlMessageParser implements WechatInboundMessageParser {
    @Override
    public WechatInboundMessage parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "WeChat callback body is empty");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setExpandEntityReferences(false);
            Element root = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)))
                    .getDocumentElement();
            Map<String, String> values = new HashMap<>();
            for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                if (root.getChildNodes().item(i) instanceof Element element) {
                    values.put(element.getTagName(), element.getTextContent());
                }
            }
            return new WechatInboundMessage(values.get("MsgType"), values.get("Event"),
                    values.get("ToUserName"), values.get("FromUserName"), values.get("Content"),
                    values.get("MediaId"), values.get("PicUrl"), values.get("MsgId"));
        } catch (Exception exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid WeChat callback XML");
        }
    }
}
