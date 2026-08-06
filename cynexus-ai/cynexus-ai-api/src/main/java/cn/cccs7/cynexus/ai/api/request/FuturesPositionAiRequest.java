package cn.cccs7.cynexus.ai.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class FuturesPositionAiRequest {

    private FuturesPositionAiRequest() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyzeRequest {
        @JsonProperty("variety_code")
        private String varietyCode;
        @JsonProperty("instrument_code")
        private String instrumentCode;
        @JsonProperty("instrument_name")
        private String instrumentName;
        @JsonProperty("exchange_code")
        private String exchangeCode;
        @JsonProperty("trade_date")
        private String tradeDate;
        private Integer top;
        private Map<String, Object> current;
        private List<Map<String, Object>> history;
        private List<Map<String, Object>> members;
    }
}
