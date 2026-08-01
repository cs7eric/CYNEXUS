package cn.cccs7.cynexus.market.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioImportReviewDTO {
    PortfolioImportTaskDTO task;
    List<PortfolioImportCandidateDTO> candidates;
}
