package cn.cccs7.cynexus.core.application.request;

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
public class SchedulerManagementJobCategoriesRequest {

    @JsonProperty("category_ids")
    private List<String> categoryIds;
}
