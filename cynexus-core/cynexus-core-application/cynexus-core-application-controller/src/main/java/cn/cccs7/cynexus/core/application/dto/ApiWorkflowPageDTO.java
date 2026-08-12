package cn.cccs7.cynexus.core.application.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiWorkflowPageDTO {
    List<ApiWorkflowProcessDTO> items;
    long total;
    int page;
    int pageSize;
    int totalPages;
    boolean hasPrevious;
    boolean hasNext;
}
