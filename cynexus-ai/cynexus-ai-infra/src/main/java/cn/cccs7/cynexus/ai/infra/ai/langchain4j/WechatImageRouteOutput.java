package cn.cccs7.cynexus.ai.infra.ai.langchain4j;
import java.util.List;
public record WechatImageRouteOutput(String route, Double confidence, String reason, List<String> signals, List<String> warnings) { }
