package cn.cccs7.cynexus.wechat.domain.model;

public record WechatReply(String toUserName, String fromUserName, String content) {

    public String toXml() {
        return "<xml>"
                + tag("ToUserName", toUserName)
                + tag("FromUserName", fromUserName)
                + "<CreateTime>" + System.currentTimeMillis() / 1000 + "</CreateTime>"
                + tag("MsgType", "text")
                + tag("Content", content)
                + "</xml>";
    }

    private static String tag(String name, String value) {
        return "<" + name + "><![CDATA[" + cdata(value) + "]]></" + name + ">";
    }

    private static String cdata(String value) {
        return value == null ? "" : value.replace("]]>", "]]]]><![CDATA[>");
    }
}
