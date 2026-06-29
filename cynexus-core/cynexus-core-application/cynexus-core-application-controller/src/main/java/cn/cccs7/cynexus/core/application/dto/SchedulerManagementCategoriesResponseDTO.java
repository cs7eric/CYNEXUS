package cn.cccs7.cynexus.core.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerManagementCategoriesResponseDTO {

    private Boolean ok;
    private List<Item> items;
    private Integer count;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String id;
        private String label;
        @JsonProperty("icon_hint")
        private String iconHint;
        @JsonProperty("sort_order")
        private Integer sortOrder;
        private String description;
        private Integer count;
    }
}
