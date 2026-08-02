package cn.cccs7.cynexus.market.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioImportTaskPageDTO {
    private List<PortfolioImportTaskDTO> items;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;
    private boolean hasPrevious;
    private boolean hasNext;
}
