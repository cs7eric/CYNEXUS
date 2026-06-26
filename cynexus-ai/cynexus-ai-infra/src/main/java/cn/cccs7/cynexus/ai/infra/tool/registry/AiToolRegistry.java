package cn.cccs7.cynexus.ai.infra.tool.registry;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiToolRegistry {

    public List<String> enabledToolCodes() {
        return List.of("market.overview", "market.sector_rank", "market.stock_daily_feature",
                "market.stock_intraday_feature");
    }
}
