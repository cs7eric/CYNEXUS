package cn.cccs7.cynexus.market.infra.cache;

import cn.cccs7.cynexus.common.cache.redis.RedisBinaryCacheOperations;
import cn.cccs7.cynexus.market.infra.client.ClickHouseStockChartClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.InflaterInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RedisTradeReviewTimeshareCache {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private RedisBinaryCacheOperations operations;

    public Optional<List<ClickHouseStockChartClient.TimesharePoint>> get(
            String targetType, String market, String symbol, LocalDate tradeDate) {
        if (operations == null || tradeDate == null || market == null || symbol == null) return Optional.empty();
        try {
            byte[] payload = operations.get(key(tradeDate, targetType, market, symbol));
            if (payload == null || payload.length < 2 || payload[0] != 'D') return Optional.empty();
            try (InputStream input = new InflaterInputStream(new ByteArrayInputStream(payload, 1, payload.length - 1))) {
                JsonNode root = objectMapper.readTree(input);
                JsonNode points = root.path("points");
                if (!points.isArray()) return Optional.empty();
                List<ClickHouseStockChartClient.TimesharePoint> result = new ArrayList<>();
                for (JsonNode point : points) {
                    if (!point.isArray() || point.size() < 5) continue;
                    int slot = point.get(0).asInt();
                    LocalTime time = slot < 120 ? LocalTime.of(9, 30).plusMinutes(slot) : LocalTime.of(13, 0).plusMinutes(slot - 120);
                    LocalDateTime timestamp = LocalDateTime.of(tradeDate, time);
                    result.add(ClickHouseStockChartClient.TimesharePoint.builder()
                            .timestamp(timestamp.atZone(SHANGHAI).toInstant().toEpochMilli())
                            .tradeDate(tradeDate)
                            .timeLabel(time.toString().substring(0, 5))
                            .price(decimal(point.get(1)))
                            .open(decimal(point.get(1)))
                            .avgPrice(point.get(2).isNull() ? null : decimal(point.get(2)))
                            .volume(decimal(point.get(3)))
                            .turnover(point.get(4).isNull() ? null : decimal(point.get(4)))
                            .build());
                }
                return result.isEmpty() ? Optional.empty() : Optional.of(result);
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private java.math.BigDecimal decimal(JsonNode node) {
        return node == null || node.isNull() ? null : node.decimalValue();
    }

    public static String key(LocalDate tradeDate, String targetType, String market, String symbol) {
        return "cynexus:market:timeshare:v1:" + tradeDate + ":" + targetType.toUpperCase() + ":"
                + market.toUpperCase() + ":" + symbol.toUpperCase();
    }
}
