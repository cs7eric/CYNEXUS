package cn.cccs7.cynexus.core.domain.mediatranscription;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.common.constant.CoreConstants;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionResolvedLinkBO;
import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

public class MediaTranscriptionProcessDomainPolicy {

    private static final Set<String> SUPPORTED_SCHEMES = Set.of("http", "https");
    private static final Set<String> DIRECT_MEDIA_EXTENSIONS = Set.of(
            "mp3", "mp4", "wav", "m4a", "aac", "flac", "ogg", "webm", "mov", "mkv");
    private static final Pattern SHARED_TEXT_URL = Pattern.compile("(?i)https?://[^\\s\\u3000]+");
    private static final String URL_TRAILING_PUNCTUATION = "。，、；;：:！!？?）)]}】>\"'";

    public String normalizeUrl(String url) {
        String normalized = extractUrl(url);
        if (normalized.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "视频链接不能为空");
        }
        try {
            URI parsed = URI.create(normalized);
            if (!SUPPORTED_SCHEMES.contains(parsed.getScheme()) || parsed.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (Exception exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, "视频链接必须是有效的 http/https URL");
        }
        return normalized;
    }

    private String extractUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return "";
        }
        Matcher matcher = SHARED_TEXT_URL.matcher(normalized);
        if (!matcher.find()) {
            return normalized;
        }
        String extracted = matcher.group();
        while (!extracted.isBlank() && URL_TRAILING_PUNCTUATION.indexOf(extracted.charAt(extracted.length() - 1)) >= 0) {
            extracted = extracted.substring(0, extracted.length() - 1);
        }
        return extracted;
    }

    public String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String normalizeStatus(String status) {
        return status == null || status.isBlank() ? CoreConstants.STATUS_QUEUED : status;
    }

    public String normalizePhase(String phase) {
        return phase == null || phase.isBlank() ? CoreConstants.STATUS_QUEUED : phase;
    }

    public int progress(Integer progress) {
        if (progress == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, progress));
    }

    public String selectDownloadUrl(MediaTranscriptionResolvedLinkBO resolved, String fallback) {
        if (resolved != null) {
            String selected = firstText(resolved.getOriginDownloadAudioUrl(), resolved.getOriginDownloadVideoUrl(),
                    resolved.getDownloadVideoUrl(), resolved.getDownloadAudioUrl());
            if (!selected.isBlank()) return selected;
        }
        return isDirectMediaUrl(fallback) ? fallback.trim() : "";
    }

    public String requireResolvedMediaUrl(MediaTranscriptionResolvedLinkBO resolved, String fallback) {
        String selected = selectDownloadUrl(resolved, fallback);
        if (selected.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "解析链接失败，未拿到视频或音频地址");
        }
        return selected;
    }

    public String selectAudioUrl(MediaTranscriptionResolvedLinkBO resolved) {
        if (resolved == null) return "";
        return firstText(resolved.getOriginDownloadAudioUrl(), resolved.getDownloadAudioUrl());
    }

    private boolean isDirectMediaUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            String path = URI.create(value.trim()).getPath();
            if (path == null) {
                return false;
            }
            int dot = path.lastIndexOf('.');
            if (dot < 0 || dot == path.length() - 1) {
                return false;
            }
            return DIRECT_MEDIA_EXTENSIONS.contains(path.substring(dot + 1).toLowerCase(Locale.ROOT));
        } catch (Exception exception) {
            return false;
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }
}
