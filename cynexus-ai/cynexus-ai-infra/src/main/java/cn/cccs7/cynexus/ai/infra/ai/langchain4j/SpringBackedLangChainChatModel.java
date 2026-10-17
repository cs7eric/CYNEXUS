package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import java.util.List;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;
import java.net.URI;
import java.util.ArrayList;

/** Thin framework bridge; provider selection remains in Cynexus routing. */
final class SpringBackedLangChainChatModel implements ChatModel {
    private static final ThreadLocal<dev.langchain4j.model.output.TokenUsage> LAST_USAGE = new ThreadLocal<>();
    private final org.springframework.ai.chat.model.ChatModel delegate;
    SpringBackedLangChainChatModel(org.springframework.ai.chat.model.ChatModel delegate) { this.delegate = delegate; }

    @Override public ChatResponse doChat(ChatRequest request) {
        String text = request.messages().stream().map(SpringBackedLangChainChatModel::text).reduce("", (a, b) -> a.isBlank() ? b : a + "\n" + b);
        List<Media> media = new ArrayList<>();
        request.messages().forEach(message -> {
            if (message instanceof dev.langchain4j.data.message.UserMessage user) {
                user.contents().stream().filter(c -> c instanceof ImageContent).map(c -> (ImageContent) c).forEach(image -> {
                    var source = image.image();
                    String value = source.base64Data() != null ? "data:" + source.mimeType() + ";base64," + source.base64Data() : source.url().toString();
                    media.add(new Media(MimeTypeUtils.parseMimeType(source.mimeType() == null ? "image/jpeg" : source.mimeType()), URI.create(value)));
                });
            }
        });
        var response = media.isEmpty() ? delegate.call(new Prompt(text)) : delegate.call(new Prompt(UserMessage.builder().text(text).media(media).build()));
        String output = response == null || response.getResult() == null || response.getResult().getOutput() == null ? "" : response.getResult().getOutput().getText();
        var usage = response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
        // Providers are allowed to omit individual usage fields. LangChain4j's
        // TokenUsage constructor uses primitive values, so normalize nulls here
        // instead of turning a successful model response into a fallback.
        var tokens = usage == null ? null : new dev.langchain4j.model.output.TokenUsage(
                nonNegative(usage.getPromptTokens()), nonNegative(usage.getCompletionTokens()),
                nonNegative(usage.getTotalTokens()));
        LAST_USAGE.set(tokens);
        return ChatResponse.builder().aiMessage(AiMessage.from(output)).tokenUsage(tokens).modelName(response == null ? null : response.getMetadata().getModel()).build();
    }

    static dev.langchain4j.model.output.TokenUsage consumeLastUsage() {
        var value = LAST_USAGE.get();
        LAST_USAGE.remove();
        return value;
    }

    private static int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private static String text(ChatMessage message) {
        if (message instanceof dev.langchain4j.data.message.SystemMessage system) return system.text();
        if (message instanceof dev.langchain4j.data.message.UserMessage user) return user.contents().stream()
                .filter(c -> c instanceof dev.langchain4j.data.message.TextContent)
                .map(c -> ((dev.langchain4j.data.message.TextContent) c).text()).reduce("", (a, b) -> a.isBlank() ? b : a + "\n" + b);
        if (message instanceof AiMessage ai) return ai.text();
        return message.toString();
    }
}
