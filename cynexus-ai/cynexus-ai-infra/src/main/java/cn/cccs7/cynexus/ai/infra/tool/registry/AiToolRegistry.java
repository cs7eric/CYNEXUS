package cn.cccs7.cynexus.ai.infra.tool.registry;

import cn.cccs7.cynexus.ai.domain.port.AiToolCatalog;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiToolRegistry implements AiToolCatalog {

    @Override
    public List<String> enabledToolCodes() {
        return List.of("market.overview", "market.sector_rank", "market.stock_daily_feature",
                "market.stock_intraday_feature");
    }
}
