package cn.cccs7.cynexus.market.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListBO;
import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListItemBO;
import cn.cccs7.cynexus.market.domain.service.marketdata.MarketDataCrudDomainService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Explicit preview -> confirm boundary for next-day attention items. */
@Service
public class MarketIndicatorWatchlistAppService {
    private static final String LIST_KIND = "market_indicator_review";
    private final MarketDataCrudDomainService marketData;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Map<String, Object>> previews = new ConcurrentHashMap<>();

    public MarketIndicatorWatchlistAppService(MarketDataCrudDomainService marketData) {
        this.marketData = marketData;
    }

    public Map<String, Object> preview(Map<String, Object> request) {
        List<Map<String, Object>> items = items(request);
        if (items.isEmpty() || items.size() > 50) {
            throw new IllegalArgumentException("items must contain 1..50 attention targets");
        }
        String token = UUID.randomUUID().toString();
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("previewToken", token);
        preview.put("expiresAt", OffsetDateTime.now().plusMinutes(10).toString());
        preview.put("targetTradeDate", request.getOrDefault("targetTradeDate", ""));
        preview.put("items", items);
        preview.put("disclaimer", "仅保存为次日关注清单，不构成无条件买卖指令");
        previews.put(token, preview);
        return preview;
    }

    public Map<String, Object> confirm(Map<String, Object> request) {
        String token = String.valueOf(request.getOrDefault("previewToken", ""));
        Map<String, Object> preview = previews.remove(token);
        if (preview == null) {
            throw new IllegalArgumentException("previewToken is missing or expired");
        }
        SelfSelectedListBO list = marketData.listSelfSelectedLists().stream()
                .filter(item -> LIST_KIND.equals(item.getListKind()) && "active".equalsIgnoreCase(item.getStatus()))
                .findFirst()
                .orElseGet(() -> {
                    SelfSelectedListBO value = SelfSelectedListBO.builder()
                            .name("Market Indicator 次日关注")
                            .description("由盘后板块传导复盘生成，保存证据与失效条件")
                            .color("orange").listKind(LIST_KIND).status("active").sortOrder(0).extra("{}").build();
                    Long id = marketData.createSelfSelectedList(value);
                    value.setId(id);
                    return value;
                });
        List<SelfSelectedListItemBO> existing = marketData.listSelfSelectedListItems();
        List<Long> itemIds = new ArrayList<>();
        for (Map<String, Object> item : items(preview)) {
            String symbol = String.valueOf(item.getOrDefault("targetCode", item.getOrDefault("code", ""))).trim();
            if (symbol.isBlank()) continue;
            boolean duplicate = existing.stream().anyMatch(existingItem -> list.getId().equals(existingItem.getListId())
                    && symbol.equalsIgnoreCase(String.valueOf(existingItem.getSymbol()))
                    && "active".equalsIgnoreCase(existingItem.getStatus()));
            if (duplicate) continue;
            SelfSelectedListItemBO value = SelfSelectedListItemBO.builder()
                    .listId(list.getId()).symbol(symbol).name(String.valueOf(item.getOrDefault("targetName", item.getOrDefault("name", symbol))))
                    .market(String.valueOf(item.getOrDefault("market", "")))
                    .targetType("stock".equalsIgnoreCase(String.valueOf(item.getOrDefault("targetType", "stock"))) ? "stock" : "other")
                    .sourceType("imported").status("active").sortOrder(existing.size()).notes(String.valueOf(item.getOrDefault("reason", "盘后传导复盘关注")))
                    .extra(json(item)).build();
            itemIds.add(marketData.createSelfSelectedListItem(value));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("listId", list.getId());
        result.put("itemIds", itemIds);
        result.put("targetTradeDate", preview.get("targetTradeDate"));
        result.put("audit", Map.of("confirmedAt", OffsetDateTime.now().toString(), "source", "market_indicator_review"));
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> request) {
        Object value = request.get("items");
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList();
    }

    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
