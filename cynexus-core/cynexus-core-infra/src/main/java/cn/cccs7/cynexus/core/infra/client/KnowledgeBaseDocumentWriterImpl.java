package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.domain.client.KnowledgeBaseDocumentWriter;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionAiResultBO;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeCategoryDTO;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeDocumentDTO;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeTagDTO;
import cn.cccs7.cynexus.knowledgebase.api.feign.KnowledgeBaseFeignClient;
import cn.cccs7.cynexus.knowledgebase.common.constant.KnowledgeBaseConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseDocumentWriterImpl implements KnowledgeBaseDocumentWriter {
    private static final String DEFAULT_TENANT = "default";
    private static final String DEFAULT_OWNER = "default";
    private final KnowledgeBaseFeignClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public KnowledgeBaseDocumentWriterImpl(KnowledgeBaseFeignClient client) { this.client = client; }

    @Override
    public Long createDraft(String taskId, String transcript, MediaTranscriptionAiResultBO ai, String sourceUrl) {
        String marker = "mp4_task_id:" + taskId;
        Result<List<KnowledgeDocumentDTO>> existing = client.listDocuments(marker, KnowledgeBaseConstants.STATUS_DRAFT, null, null);
        if (existing != null && !existing.isFailed() && existing.getData() != null && !existing.getData().isEmpty()) {
            KnowledgeDocumentDTO current = existing.getData().getFirst();
            Long documentId = current.getId();
            List<Long> categoryIds = resolveCategories(ai.categories());
            List<Long> tagIds = resolveTags(ai.tags());
            String title = limitTitle(ai.title() == null || ai.title().isBlank() ? "MP4 转写 " + taskId : ai.title());
            String content = buildContent(transcript, ai);
            Result<Boolean> updated = client.updateDocument(KnowledgeDocumentDTO.builder().id(documentId).title(title)
                    .description(summaryDescription(marker, ai))
                    .content(content).contentFormat(KnowledgeBaseConstants.CONTENT_FORMAT_MARKDOWN)
                    .status(KnowledgeBaseConstants.STATUS_DRAFT.toUpperCase()).tagIds(tagIds).categoryIds(categoryIds)
                    .tenantKey(DEFAULT_TENANT).ownerKey(DEFAULT_OWNER)
                    .extra(metadata(taskId, sourceUrl)).build());
            if (updated != null && !updated.isFailed() && Boolean.TRUE.equals(updated.getData())) return documentId;
        }
        List<Long> categoryIds = resolveCategories(ai.categories());
        List<Long> tagIds = resolveTags(ai.tags());
        String title = limitTitle(ai.title() == null || ai.title().isBlank() ? "MP4 转写 " + taskId : ai.title());
        String content = buildContent(transcript, ai);
        try {
            Result<Long> created = client.createDocument(KnowledgeDocumentDTO.builder().title(title)
                    .description(summaryDescription(marker, ai)).content(content).contentFormat(KnowledgeBaseConstants.CONTENT_FORMAT_MARKDOWN)
                    .status(KnowledgeBaseConstants.STATUS_DRAFT.toUpperCase()).tagIds(tagIds).categoryIds(categoryIds)
                    .tenantKey(DEFAULT_TENANT).ownerKey(DEFAULT_OWNER)
                    .extra(metadata(taskId, sourceUrl)).build());
            if (created == null || created.isFailed() || created.getData() == null) throw new IllegalStateException("knowledge base rejected draft");
            return created.getData();
        } catch (Exception exception) { throw new IllegalStateException("knowledge base draft creation failed", exception); }
    }

    private List<Long> resolveCategories(List<String> names) {
        List<Long> ids = new ArrayList<>();
        if (names == null) return ids;
        for (String name : names) { if (name == null || name.isBlank()) continue; Result<List<KnowledgeCategoryDTO>> found = client.listCategories(name); if (found != null && found.getData() != null && !found.getData().isEmpty()) ids.add(found.getData().getFirst().getId()); else { Result<Long> created = client.createCategory(KnowledgeCategoryDTO.builder().categoryCode(code(name)).categoryName(name).build()); if (created != null && created.getData() != null) ids.add(created.getData()); } }
        return ids;
    }

    private List<Long> resolveTags(List<String> names) {
        List<Long> ids = new ArrayList<>();
        if (names == null) return ids;
        for (String name : names) { if (name == null || name.isBlank()) continue; Result<List<KnowledgeTagDTO>> found = client.listTags(name); if (found != null && found.getData() != null && !found.getData().isEmpty()) ids.add(found.getData().getFirst().getId()); else { Result<Long> created = client.createTag(KnowledgeTagDTO.builder().tagCode(code(name)).tagName(name).build()); if (created != null && created.getData() != null) ids.add(created.getData()); } }
        return ids;
    }

    private String buildContent(String transcript, MediaTranscriptionAiResultBO ai) {
        StringBuilder content = new StringBuilder();
        appendText(content, "## 一句话总结", ai.shortSummary());
        appendText(content, "## 摘要", ai.summary());
        appendList(content, "## 核心观点", ai.keyPoints(), false);
        appendList(content, "## 方法论", ai.methodology(), false);
        appendList(content, "## 可执行清单", ai.actionItems(), true);
        appendList(content, "## 风险与边界", ai.risks(), false);
        appendText(content, "## 润色稿", ai.polishedText());
        if (transcript != null && !transcript.isBlank()) {
            content.append("\n---\n\n<details>\n<summary>原始 Transcript</summary>\n\n")
                    .append(transcript.trim()).append("\n\n</details>\n");
        }
        return content.toString().trim() + "\n";
    }
    private void appendText(StringBuilder content, String heading, String value) {
        if (value != null && !value.isBlank()) content.append(heading).append("\n\n").append(value.trim()).append("\n\n");
    }
    private void appendList(StringBuilder content, String heading, List<String> values, boolean checklist) {
        if (values == null || values.isEmpty()) return;
        content.append(heading).append("\n\n");
        for (String value : values) {
            if (value != null && !value.isBlank()) content.append(checklist ? "- [ ] " : "- ").append(value.trim()).append("\n");
        }
        content.append("\n");
    }
    private String summaryDescription(String marker, MediaTranscriptionAiResultBO ai) {
        String summary = ai.shortSummary() == null ? "" : ai.shortSummary().trim();
        return summary.isBlank() ? marker : marker + "\n" + summary;
    }
    private String metadata(String taskId, String sourceUrl) {
        try {
            return mapper.writeValueAsString(Map.of("source", "mp4", "mp4_task_id", taskId,
                    "source_url", sourceUrl == null ? "" : sourceUrl, "tenantKey", DEFAULT_TENANT,
                    "ownerKey", DEFAULT_OWNER, "sourceType", "mp4", "permissionScope", "owner"));
        } catch (Exception exception) {
            throw new IllegalStateException("knowledge base metadata serialization failed", exception);
        }
    }
    private String code(String value) { return value.trim().toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fff]+", "-"); }
    private String limitTitle(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 200).trim();
    }
}
