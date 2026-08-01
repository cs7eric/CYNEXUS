package cn.cccs7.cynexus.market.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioImportCandidateDTO {
    long id;
    int rowNo;
    String normalizedPayload;
    String reviewStatus;
    long version;
    Double confidence;
    List<String> warnings;
}
