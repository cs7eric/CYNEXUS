package cn.cccs7.cynexus.core.domain.apiworkflow;

import cn.cccs7.cynexus.core.common.constant.CoreConstants;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionProcessBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureBO;
import cn.cccs7.cynexus.core.domain.finance.port.FinanceRepository;
import cn.cccs7.cynexus.core.domain.repository.MediaTranscriptionProcessRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class ApiWorkflowQueryDomainServiceImpl implements ApiWorkflowQueryDomainService {
    private static final String OWNER_KEY = "default";

    private final MediaTranscriptionProcessRepository mediaRepository;
    private final FinanceRepository financeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiWorkflowQueryDomainServiceImpl(MediaTranscriptionProcessRepository mediaRepository,
                                             FinanceRepository financeRepository) {
        this.mediaRepository = mediaRepository;
        this.financeRepository = financeRepository;
    }

    @Override
    public ApiWorkflowPageBO list(int page, int pageSize, ApiWorkflowType type, ApiWorkflowStatus status) {
        int normalizedPage = Math.max(1, Math.min(page, 100));
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 2_000));
        int requested = Math.min(2_000, Math.max(normalizedPageSize,
                (int) Math.min(2_000L, (long) normalizedPage * normalizedPageSize)));
        String mediaStatus = mediaStatus(status);
        String financeStatus = financeStatus(status);
        List<ApiWorkflowProcessBO> result = new ArrayList<>();
        if (type == null || type == ApiWorkflowType.MEDIA_TRANSCRIPTION) {
            mediaRepository.listActive(OWNER_KEY, requested, 0, mediaStatus).stream()
                    .map(this::media)
                    .filter(item -> status == null || item.status() == status)
                    .forEach(result::add);
        }
        if (type == null || type == ApiWorkflowType.FINANCE_AI_CAPTURE) {
            financeRepository.listCaptures(OWNER_KEY, requested, 0, financeStatus == null ? "all" : financeStatus).stream()
                    .map(this::finance)
                    .filter(item -> status == null || item.status() == status)
                    .forEach(result::add);
        }
        long total = 0;
        if (type == null || type == ApiWorkflowType.MEDIA_TRANSCRIPTION) total += mediaRepository.countActive(OWNER_KEY, mediaStatus);
        if (type == null || type == ApiWorkflowType.FINANCE_AI_CAPTURE) total += financeRepository.countCaptures(OWNER_KEY, financeStatus == null ? "all" : financeStatus);
        int offset = (normalizedPage - 1) * normalizedPageSize;
        List<ApiWorkflowProcessBO> items = result.stream()
                .sorted(Comparator.comparing(ApiWorkflowProcessBO::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .skip(offset)
                .limit(normalizedPageSize)
                .toList();
        return new ApiWorkflowPageBO(items, total, normalizedPage, normalizedPageSize);
    }

    private ApiWorkflowProcessBO media(MediaTranscriptionProcessBO process) {
        ApiWorkflowStatus status = mapMediaStatus(process.getStatus());
        String phase = value(process.getPhase(), "queued");
        String resultRef = process.getHistoryId() == null ? null : String.valueOf(process.getHistoryId());
        List<ApiWorkflowEventBO> events = new ArrayList<>();
        events.add(event("QUEUED", "queued", ApiWorkflowStatus.QUEUED, 0, "Request queued", null, null, process.getCreatedAt()));
        if (process.getStartedAt() != null) {
            events.add(event("PROCESSING_STARTED", "starting", ApiWorkflowStatus.RUNNING, Math.max(2, value(process.getProgress(), 0)),
                    "Background processing started", null, null, process.getStartedAt()));
        }
        if (process.getCompletedAt() != null && status == ApiWorkflowStatus.SUCCEEDED) {
            JsonNode analysis = parseAnalysis(process.getMetadataJson());
            String summary = text(analysis, "shortSummary");
            String categories = join(analysis, "categories");
            events.add(event("AI_ENRICHED", "ai_enriching", status, 76, "AI enrichment completed",
                    null, null, compactDetails(summary, categories), process.getCompletedAt()));
            String knowledgeStatus = text(analysis, "knowledgeIngestionStatus");
            if ("SUCCEEDED".equalsIgnoreCase(knowledgeStatus)) {
                String documentId = text(analysis, "knowledgeDocumentId");
                events.add(event("KNOWLEDGE_BASE_WRITTEN", "knowledge_base", status, 92, "Knowledge base draft created",
                        null, null, documentId.isBlank() ? null : "Document ID: " + documentId, process.getCompletedAt()));
            } else if ("FAILED".equalsIgnoreCase(knowledgeStatus)) {
                events.add(event("KNOWLEDGE_BASE_FAILED", "knowledge_base", status, 92, "Knowledge base write failed; AI result retained",
                        null, text(analysis, "knowledgeIngestionError"), null, process.getCompletedAt()));
            }
            events.add(event("RESULT_READY", "completed", status, 100, "Transcript saved to history", null, null, process.getCompletedAt()));
        } else if (status == ApiWorkflowStatus.FAILED) {
            events.add(event("FAILED", phase, status, value(process.getProgress(), 0), "Processing failed", null,
                    process.getErrorMessage(), process.getCompletedAt() == null ? process.getUpdatedAt() : process.getCompletedAt()));
        } else if (process.getStartedAt() != null) {
            events.add(event("STEP_UPDATED", phase, status, value(process.getProgress(), 0), "Current step: " + phase, null, null, process.getUpdatedAt()));
        }
        return new ApiWorkflowProcessBO(String.valueOf(process.getId()), ApiWorkflowType.MEDIA_TRANSCRIPTION,
                "media.transcription.url", "api", String.valueOf(process.getId()),
                value(process.getTitle(), "视频转写"), status, phase, value(process.getProgress(), 0),
                value(process.getAttemptCount(), 0), null, process.getErrorMessage(),
                resultRef == null ? null : "/mp4-to-word/history/" + resultRef, resultRef,
                process.getCreatedAt(), process.getStartedAt(), process.getCompletedAt(), process.getUpdatedAt(), events);
    }

    private ApiWorkflowProcessBO finance(FinanceAiCaptureBO capture) {
        ApiWorkflowStatus status = switch (value(capture.getStatus(), "failed")) {
            case "uploaded", "queued" -> ApiWorkflowStatus.QUEUED;
            case "processing" -> ApiWorkflowStatus.RUNNING;
            case "review" -> ApiWorkflowStatus.WAITING_REVIEW;
            case "confirmed" -> ApiWorkflowStatus.SUCCEEDED;
            case "archived" -> ApiWorkflowStatus.CANCELLED;
            default -> ApiWorkflowStatus.FAILED;
        };
        String phase = switch (status) {
            case QUEUED -> "queued";
            case RUNNING -> "ai_processing";
            case WAITING_REVIEW -> "waiting_review";
            case SUCCEEDED -> "completed";
            case CANCELLED -> "archived";
            case FAILED -> "failed";
        };
        List<ApiWorkflowEventBO> events = new ArrayList<>();
        events.add(event("QUEUED", "queued", ApiWorkflowStatus.QUEUED, 0, "图片识别请求已创建", null, null, capture.getCreatedAt()));
        if (status == ApiWorkflowStatus.RUNNING || status == ApiWorkflowStatus.WAITING_REVIEW || status == ApiWorkflowStatus.SUCCEEDED) {
            events.add(event("AI_STARTED", "ai_processing", ApiWorkflowStatus.RUNNING, 50, "AI 正在识别图片", null, null, capture.getUpdatedAt()));
        }
        if (status == ApiWorkflowStatus.WAITING_REVIEW) {
            events.add(event("WAITING_REVIEW", phase, status, 90, "识别结果等待确认", null, null, capture.getUpdatedAt()));
        } else if (status == ApiWorkflowStatus.SUCCEEDED) {
            events.add(event("RESULT_READY", "completed", status, 100, "账目结果已准备完成", null, null, capture.getUpdatedAt()));
        } else if (status == ApiWorkflowStatus.FAILED) {
            events.add(event("FAILED", phase, status, 0, "图片识别失败", null, capture.getErrorMessage(), capture.getUpdatedAt()));
        }
        String resultRef = capture.getId() == null ? null : String.valueOf(capture.getId());
        return new ApiWorkflowProcessBO(resultRef, ApiWorkflowType.FINANCE_AI_CAPTURE, "finance.ai_capture.image", "api",
                resultRef, "AI 入账", status, phase, status == ApiWorkflowStatus.SUCCEEDED ? 100 : status == ApiWorkflowStatus.WAITING_REVIEW ? 90 : status == ApiWorkflowStatus.RUNNING ? 50 : 0,
                1, null, capture.getErrorMessage(), "/finance/ai-capture?captureId=" + resultRef, resultRef,
                capture.getCreatedAt(), capture.getCreatedAt(), status == ApiWorkflowStatus.FAILED || status == ApiWorkflowStatus.SUCCEEDED ? capture.getUpdatedAt() : null,
                capture.getUpdatedAt(), events);
    }

    private static ApiWorkflowEventBO event(String type, String phase, ApiWorkflowStatus status, int progress,
                                             String message, String errorCode, String errorMessage, OffsetDateTime occurredAt) {
        return new ApiWorkflowEventBO(0, type, null, status == null ? null : status.name(), phase, status, progress,
                message, null, null, errorCode, errorMessage, null, null, occurredAt);
    }

    private static ApiWorkflowEventBO event(String type, String phase, ApiWorkflowStatus status, int progress,
                                             String message, String errorCode, String errorMessage, String details,
                                             OffsetDateTime occurredAt) {
        return new ApiWorkflowEventBO(0, type, null, status == null ? null : status.name(), phase, status, progress,
                message, null, null, errorCode, errorMessage, details, null, occurredAt);
    }

    private JsonNode parseAnalysis(String json) {
        try { return json == null || json.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(json); }
        catch (Exception ignored) { return objectMapper.createObjectNode(); }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private static String join(JsonNode node, String field) {
        JsonNode values = node == null ? null : node.get(field);
        if (values == null || !values.isArray()) return "";
        List<String> result = new ArrayList<>();
        values.forEach(value -> { if (!value.isNull() && !value.asText("").isBlank()) result.add(value.asText().trim()); });
        return String.join("、", result);
    }

    private static String compactDetails(String summary, String categories) {
        if (!summary.isBlank() && !categories.isBlank()) return "摘要: " + summary + "；分类: " + categories;
        if (!summary.isBlank()) return "摘要: " + summary;
        return categories.isBlank() ? null : "分类: " + categories;
    }

    private static ApiWorkflowStatus mapMediaStatus(String status) {
        if (CoreConstants.STATUS_QUEUED.equalsIgnoreCase(status)) return ApiWorkflowStatus.QUEUED;
        if (CoreConstants.STATUS_RUNNING.equalsIgnoreCase(status)) return ApiWorkflowStatus.RUNNING;
        if (CoreConstants.STATUS_FAILED.equalsIgnoreCase(status)) return ApiWorkflowStatus.FAILED;
        return ApiWorkflowStatus.SUCCEEDED;
    }

    private static String mediaStatus(ApiWorkflowStatus status) {
        if (status == null) return null;
        return switch (status) {
            case QUEUED -> "queued";
            case RUNNING -> "running";
            case FAILED -> "failed";
            case SUCCEEDED -> "succeeded";
            default -> "__no_match__";
        };
    }

    private static String financeStatus(ApiWorkflowStatus status) {
        if (status == null) return null;
        return switch (status) {
            case QUEUED -> "queued";
            case RUNNING -> "processing";
            case WAITING_REVIEW -> "review";
            case SUCCEEDED -> "confirmed";
            case CANCELLED -> "archived";
            case FAILED -> "failed";
        };
    }

    private static String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static int value(Integer value, int fallback) { return value == null ? fallback : value; }
}
