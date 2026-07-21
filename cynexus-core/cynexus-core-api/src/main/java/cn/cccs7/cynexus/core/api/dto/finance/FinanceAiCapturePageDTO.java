package cn.cccs7.cynexus.core.api.dto.finance;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceAiCapturePageDTO {
    private List<FinanceAiCaptureDTO> items;
    private long total;
    private int limit;
    private int offset;
    private String status;
}
