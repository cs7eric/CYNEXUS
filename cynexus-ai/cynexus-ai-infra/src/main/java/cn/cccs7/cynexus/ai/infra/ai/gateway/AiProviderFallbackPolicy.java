package cn.cccs7.cynexus.ai.infra.ai.gateway;

import cn.cccs7.cynexus.ai.infra.ai.adapter.OpenAiCompatibleChatAdapter.ProviderHttpException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only transport/provider availability failures may trigger the rollback adapter. */
final class AiProviderFallbackPolicy {
    private static final Pattern STATUS = Pattern.compile("(?:http|status|response)[^0-9]{0,12}(\\d{3})");

    private AiProviderFallbackPolicy() { }

    static boolean isEligible(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ProviderHttpException providerFailure) {
                int status = providerFailure.statusCode();
                return status == 429 || status >= 500;
            }
            if (current instanceof TimeoutException || current instanceof SocketTimeoutException
                    || current instanceof ConnectException || current instanceof IOException) {
                return true;
            }
            current = current.getCause();
        }
        String message = failure == null || failure.getMessage() == null
                ? "" : failure.getMessage().toLowerCase(Locale.ROOT);
        Matcher status = STATUS.matcher(message);
        if (status.find()) {
            int code = Integer.parseInt(status.group(1));
            return code == 429 || code >= 500;
        }
        return message.contains("timed out") || message.contains("connection refused")
                || message.contains("connection reset") || message.contains("temporarily unavailable");
    }
}
