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
import org.springframework.stereotype.Service;

@Service
public class ApiWorkflowQueryDomainServiceImpl implements ApiWorkflowQueryDomainService {
    private static final String OWNER_KEY = "default";

    private final MediaTranscriptionProcessRepository mediaRepository;
    private final FinanceRepository financeRepository;

    public ApiWorkflowQueryDomainServiceImpl(MediaTranscriptionProcessRepository mediaRepository,
                                             FinanceRepository financeRepository) {
        this.mediaRepository = mediaRepository;
        this.financeRepository = financeRepository;
    }

    @Override
    public List<ApiWorkflowProcessBO> list(int limit, ApiWorkflowType type, ApiWorkflowStatus status) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        List<ApiWorkflowProcessBO> result = new ArrayList<>();
        if (type == null || type == ApiWorkflowType.MEDIA_TRANSCRIPTION) {
            mediaRepository.listActive(OWNER_KEY, normalizedLimit).stream()
                    .map(this::media)
                    .filter(item -> status == null || item.status() == status)
                    .forEach(result::add);
        }
        if (type == null || type == ApiWorkflowType.FINANCE_AI_CAPTURE) {
            financeRepository.listCaptures(OWNER_KEY, normalizedLimit, 0, "all").stream()
                    .map(this::finance)
                    .filter(item -> status == null || item.status() == status)
                    .forEach(result::add);
        }
        return result.stream()
                .sorted(Comparator.comparing(ApiWorkflowProcessBO::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(normalizedLimit)
                .toList();
    }

    private ApiWorkflowProcessBO media(MediaTranscriptionProcessBO process) {
        ApiWorkflowStatus status = mapMediaStatus(process.getStatus());
        String phase = value(process.getPhase(), "queued");
        String resultRef = process.getHistoryId() == null ? null : String.valueOf(process.getHistoryId());
        List<ApiWorkflowEventBO> events = new ArrayList<>();
        events.add(event("QUEUED", "queued", ApiWorkflowStatus.QUEUED, 0, "请求已入队", null, null, process.getCreatedAt()));
        if (process.getStartedAt() != null) {
            events.add(event("PROCESSING_STARTED", "starting", ApiWorkflowStatus.RUNNING, Math.max(2, value(process.getProgress(), 0)),
                    "后台处理已开始", null, null, process.getStartedAt()));
        }
        if (process.getCompletedAt() != null && status == ApiWorkflowStatus.SUCCEEDED) {
            events.add(event("RESULT_READY", "completed", status, 100, "转写结果已写入历史", null, null, process.getCompletedAt()));
        } else if (status == ApiWorkflowStatus.FAILED) {
            events.add(event("FAILED", phase, status, value(process.getProgress(), 0), "处理失败", null,
                    process.getErrorMessage(), process.getCompletedAt() == null ? process.getUpdatedAt() : process.getCompletedAt()));
        } else if (process.getStartedAt() != null) {
            events.add(event("STEP_UPDATED", phase, status, value(process.getProgress(), 0), "当前步骤：" + phase, null, null, process.getUpdatedAt()));
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
            case "uploaded" -> ApiWorkflowStatus.QUEUED;
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

    private static ApiWorkflowStatus mapMediaStatus(String status) {
        if (CoreConstants.STATUS_QUEUED.equalsIgnoreCase(status)) return ApiWorkflowStatus.QUEUED;
        if (CoreConstants.STATUS_RUNNING.equalsIgnoreCase(status)) return ApiWorkflowStatus.RUNNING;
        if (CoreConstants.STATUS_FAILED.equalsIgnoreCase(status)) return ApiWorkflowStatus.FAILED;
        return ApiWorkflowStatus.SUCCEEDED;
    }

    private static String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static int value(Integer value, int fallback) { return value == null ? fallback : value; }
}
