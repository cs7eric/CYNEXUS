package cn.cccs7.cynexus.core.application.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerManagementCategoryCreateRequest {

    private String code;
    private String label;
    @JsonProperty("icon_hint")
    private String iconHint;
    @JsonProperty("sort_order")
    private Integer sortOrder;
    private String description;
}
