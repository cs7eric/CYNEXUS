package cn.cccs7.cynexus.core.domain.mediatranscription.impl;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.domain.client.MediaTranscriptionAiClient;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskBO;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionDomainPolicy;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionHistoryDomainService;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionHistoryQaBO;
import cn.cccs7.cynexus.core.domain.repository.MediaTranscriptionHistoryRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class MediaTranscriptionHistoryDomainServiceImpl implements MediaTranscriptionHistoryDomainService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final MediaTranscriptionHistoryRepository repository;
    private final MediaTranscriptionAiClient aiClient;
    private final MediaTranscriptionDomainPolicy policy = new MediaTranscriptionDomainPolicy();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MediaTranscriptionHistoryDomainServiceImpl(MediaTranscriptionHistoryRepository repository,
                                                      MediaTranscriptionAiClient aiClient) {
        this.repository = repository;
        this.aiClient = aiClient;
    }

    @Override
    public List<MediaTranscriptionHistoryBO> list() {
        return repository.listActive();
    }

    @Override
    public MediaTranscriptionHistoryBO require(String historyId) {
        Long id = parseLong(historyId);
        if (id == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "historyId is invalid");
        }
        MediaTranscriptionHistoryBO history = repository.findById(id);
        if (history == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "历史记录不存在");
        }
        return history;
    }

    @Override
    public Map<String, Object> record(String historyId) {
        return parseRecord(require(historyId).getRecordJson());
    }

    @Override
    public MediaTranscriptionHistoryBO saveCompletedTask(MediaTranscriptionTaskBO task) {
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "转写任务不存在");
        }
        if (!policy.isDone(task)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "任务尚未完成");
        }
        MediaTranscriptionHistoryBO existing = repository.findByTaskId(task.getTaskId());
        List<Map<String, Object>> qaItems = existing == null
                ? List.of()
                : readQaItems(parseRecord(existing.getRecordJson()));
        Map<String, Object> record = policy.buildHistoryRecord(
                existing == null ? null : existing.getId(), task, qaItems, OffsetDateTime.now());
        String title = String.valueOf(record.get("title"));
        String status = policy.normalize(task.getStatus());
        if (existing == null) {
            Long id = repository.create(MediaTranscriptionHistoryBO.builder()
                    .taskId(task.getTaskId())
                    .title(title)
                    .status(status)
                    .fileName(task.getFileName())
                    .recordJson(toJson(record))
                    .sortOrder((int) (System.currentTimeMillis() / 1000L))
                    .build());
            MediaTranscriptionHistoryBO saved = repository.findById(id);
            if (saved != null) {
                Map<String, Object> savedRecord = parseRecord(saved.getRecordJson());
                savedRecord.put("id", String.valueOf(saved.getId()));
                saved.setRecordJson(toJson(savedRecord));
                repository.update(saved);
                saved = repository.findById(id);
            }
            return saved;
        }
        existing.setTitle(title);
        existing.setStatus(status);
        existing.setFileName(task.getFileName());
        existing.setRecordJson(toJson(record));
        repository.update(existing);
        return repository.findById(existing.getId());
    }

    @Override
    public List<MediaTranscriptionHistoryBO> reorder(List<String> orderedIds) {
        if (orderedIds != null) {
            int sortOrder = orderedIds.size();
            for (String orderedId : orderedIds) {
                Long id = parseLong(orderedId);
                if (id != null) {
                    repository.updateSortOrder(id, sortOrder--);
                }
            }
        }
        return list();
    }

    @Override
    public MediaTranscriptionHistoryBO delete(String historyId) {
        MediaTranscriptionHistoryBO history = require(historyId);
        repository.delete(history.getId());
        return history;
    }

    @Override
    public MediaTranscriptionHistoryQaBO ask(String historyId, String question) {
        if (question == null || question.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "问题不能为空");
        }
        MediaTranscriptionHistoryBO history = require(historyId);
        Map<String, Object> record = parseRecord(history.getRecordJson());
        Map<String, Object> task = asMap(record.get("task"));
        var result = aiClient.ask(
                policy.firstText(asString(task.get("polished")), asString(task.get("transcript"))),
                asString(task.get("summary")), question);
        OffsetDateTime now = OffsetDateTime.now();
        MediaTranscriptionHistoryQaBO item = MediaTranscriptionHistoryQaBO.builder()
                .id("qa-" + UUID.randomUUID().toString().replace("-", ""))
                .question(question)
                .answer(result == null ? null : result.getAnswer())
                .createdAt(now)
                .build();
        List<Map<String, Object>> qaItems = readQaItems(record);
        qaItems.add(0, qaItemToMap(item));
        task.put("qa_items", qaItems);
        record.put("task", task);
        record.put("updated_at", now.toString());
        history.setRecordJson(toJson(record));
        repository.update(history);
        return item;
    }

    private Map<String, Object> parseRecord(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "历史记录解析失败");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "历史记录序列化失败");
        }
    }

    private List<Map<String, Object>> readQaItems(Map<String, Object> record) {
        Map<String, Object> task = asMap(record.get("task"));
        Object value = task.get("qa_items");
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                map.forEach((key, entry) -> normalized.put(String.valueOf(key), entry));
                items.add(normalized);
            }
        }
        return items;
    }

    private Map<String, Object> qaItemToMap(MediaTranscriptionHistoryQaBO item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("question", item.getQuestion());
        map.put("answer", item.getAnswer());
        map.put("created_at", item.getCreatedAt().toString());
        return map;
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return new LinkedHashMap<>();
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
