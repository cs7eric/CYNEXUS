package cn.cccs7.cynexus.core.domain.service.scheduler.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

final class SchedulerScheduleJson {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SchedulerScheduleJson() {
    }

    static Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<?, ?> parsed = OBJECT_MAPPER.readValue(json, Map.class);
            Map<String, Object> result = new LinkedHashMap<>();
            if (parsed != null) {
                parsed.forEach((key, value) -> result.put(String.valueOf(key), value));
            }
            return result;
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
