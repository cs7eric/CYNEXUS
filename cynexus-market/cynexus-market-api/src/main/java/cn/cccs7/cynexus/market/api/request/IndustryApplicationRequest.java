package cn.cccs7.cynexus.market.api.request;

import cn.cccs7.cynexus.market.api.dto.IndustryApplicationDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

public final class IndustryApplicationRequest {

    private IndustryApplicationRequest() {
    }

    @Data
    public static class SaveTargetsRequest {
        private IndustryApplicationDTO.Horizon horizon;
        private List<IndustryApplicationDTO.Target> items;
    }

    @Data
    public static class RefreshRequest {
        @JsonProperty("target_id")
        private String targetId;
    }
}
