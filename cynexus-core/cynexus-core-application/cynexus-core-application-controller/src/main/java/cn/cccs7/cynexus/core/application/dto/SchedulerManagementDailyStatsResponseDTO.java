package cn.cccs7.cynexus.core.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerManagementDailyStatsResponseDTO {

    private Boolean ok;
    private List<Item> items;
    private Summary summary;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private LocalDate date;
        private Integer total;
        private Integer success;
        private Integer failed;
        private Integer skipped;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer total;
        private Integer failed;
        @JsonProperty("success_rate")
        private Double successRate;
    }
}
