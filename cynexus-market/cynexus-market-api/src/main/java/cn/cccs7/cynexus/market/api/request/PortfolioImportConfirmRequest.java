package cn.cccs7.cynexus.market.api.request;

import java.util.List;
import lombok.Data;

@Data
public class PortfolioImportConfirmRequest {
    private Long version;
    private List<Long> candidateIds;
}
