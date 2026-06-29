package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.StockChartDTO;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.repository.StockChartQueryRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StockChartQueryAppService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int DEFAULT_KLINE_COUNT = 520;

    private final StockChartQueryRepository stockChartQueryRepository;

    public StockChartQueryAppService(StockChartQueryRepository stockChartQueryRepository) {
        this.stockChartQueryRepository = stockChartQueryRepository;
    }

    public StockChartDTO.SearchResponseDTO search(String keyword) {
        return StockChartDTO.SearchResponseDTO.builder()
                .items(stockChartQueryRepository.search(keyword, 20).stream()
                        .map(this::searchItem)
                        .toList())
                .build();
    }

    public StockChartDTO.KlineResponseDTO klines(String targetType, String symbol, String period, String adjust) {
        String normalizedPeriod = normalizePeriod(period);
        List<StockChartBO.Bar> rows;
        String source;
        if ("1d".equals(normalizedPeriod)) {
            rows = stockChartQueryRepository.listDailyBars(targetType, symbol, DEFAULT_KLINE_COUNT);
            source = rows.isEmpty() ? "java-tdx-local-empty" : "java-tdx-local";
        } else {
            int periodMinutes = periodMinutes(normalizedPeriod);
            LocalDate effectiveDate = stockChartQueryRepository
                    .findLatestMinuteDate(targetType, symbol, LocalDate.now(SHANGHAI))
                    .orElse(null);
            rows = effectiveDate == null
                    ? List.of()
                    : stockChartQueryRepository.listMinuteBars(targetType, symbol, effectiveDate, periodMinutes);
            source = rows.isEmpty() ? "clickhouse.ch_dwd_kline_1m-empty" : "clickhouse.ch_dwd_kline_1m";
        }
        return StockChartDTO.KlineResponseDTO.builder()
                .symbol(symbol)
                .targetType(blankDefault(targetType, "stock"))
                .period(normalizedPeriod)
                .adjust(blankDefault(adjust, "qfq"))
                .updatedAt(OffsetDateTime.now(SHANGHAI))
                .source(source)
                .items(rows.stream().map(this::bar).toList())
                .build();
    }

    public StockChartDTO.StockMetaDTO stockMeta(String symbol) {
        StockChartBO.StockMeta value = stockChartQueryRepository.stockMeta(symbol);
        return StockChartDTO.StockMetaDTO.builder()
                .symbol(value.getSymbol())
                .name(value.getName())
                .totalMarketCap(value.getTotalMarketCap())
                .circMarketCap(value.getCircMarketCap())
                .industry(value.getIndustry())
                .capStyle(value.getCapStyle())
                .sectorIndexSymbol(value.getSectorIndexSymbol())
                .sectorIndexName(value.getSectorIndexName())
                .build();
    }

    public StockChartDTO.FundFlowSnapshotDTO fundFlow(String symbol) {
        StockChartBO.FundFlowSnapshot value = stockChartQueryRepository.fundFlow(symbol);
        return StockChartDTO.FundFlowSnapshotDTO.builder()
                .ok(value.getOk())
                .symbol(value.getSymbol())
                .name(value.getName())
                .tradeDate(value.getTradeDate())
                .lastPrice(value.getLastPrice())
                .changePct(value.getChangePct())
                .amountWan(value.getAmountWan())
                .turnoverRate(value.getTurnoverRate())
                .activeNetAmountWan(value.getActiveNetAmountWan())
                .activeBuyRatio(value.getActiveBuyRatio())
                .activeSellRatio(value.getActiveSellRatio())
                .buyBigRatio(value.getBuyBigRatio())
                .sellBigRatio(value.getSellBigRatio())
                .source(value.getSource())
                .capturedAt(value.getCapturedAt())
                .error(value.getError())
                .history(value.getHistory() == null ? List.of() : value.getHistory().stream().map(this::fundFlowPoint).toList())
                .build();
    }

    public StockChartDTO.IntradayDTO intraday(String targetType, String symbol, String name, String adjust,
                                              String tradeDate, List<String> periods) {
        LocalDate requested = parseDateOrToday(tradeDate);
        LocalDate effectiveDate = stockChartQueryRepository.findLatestMinuteDate(targetType, symbol, requested).orElse(null);
        if (effectiveDate == null) {
            return StockChartDTO.IntradayDTO.builder()
                    .ok(true)
                    .symbol(symbol)
                    .targetType(blankDefault(targetType, "stock"))
                    .name(blankDefault(name, symbol))
                    .adjust(blankDefault(adjust, "qfq"))
                    .effectiveAdjust("none")
                    .requestedAdjust(blankDefault(adjust, "qfq"))
                    .source("clickhouse.ch_dwd_kline_1m-empty")
                    .requestedTradeDate(requested)
                    .timeshare(List.of())
                    .minuteBars(emptyMinuteBars())
                    .error("minute kline / timeshare data not found")
                    .build();
        }

        Map<String, List<StockChartDTO.KlineBarDTO>> minuteBars = new HashMap<>();
        for (String period : periods) {
            int minutes = periodMinutes(period);
            minuteBars.put(period, stockChartQueryRepository.listMinuteBars(targetType, symbol, effectiveDate, minutes).stream()
                    .map(this::bar)
                    .toList());
        }
        List<StockChartDTO.IntradayPointDTO> timeshare = stockChartQueryRepository.listTimeshare(targetType, symbol, effectiveDate).stream()
                .map(this::intradayPoint)
                .toList();
        boolean empty = timeshare.isEmpty() && minuteBars.values().stream().allMatch(List::isEmpty);
        return StockChartDTO.IntradayDTO.builder()
                .ok(true)
                .symbol(symbol)
                .targetType(blankDefault(targetType, "stock"))
                .name(blankDefault(name, symbol))
                .adjust(blankDefault(adjust, "qfq"))
                .effectiveAdjust("none")
                .requestedAdjust(blankDefault(adjust, "qfq"))
                .source(empty ? "clickhouse.ch_dwd_kline_1m-empty" : "clickhouse.ch_dwd_kline_1m")
                .tradeDate(effectiveDate)
                .requestedTradeDate(requested)
                .timeshare(timeshare)
                .minuteBars(minuteBars(minuteBars))
                .error(empty ? "minute kline / timeshare data not found" : null)
                .build();
    }

    public StockChartDTO.AuctionDTO auction(String symbol) {
        return StockChartDTO.AuctionDTO.builder()
                .symbol(symbol)
                .tradeDate(LocalDate.now(SHANGHAI))
                .opening(StockChartDTO.AuctionPhaseDTO.builder().dataConfidence("not_wired").build())
                .closing(StockChartDTO.AuctionPhaseDTO.builder().dataConfidence("not_wired").build())
                .details(StockChartDTO.AuctionDetailsDTO.builder()
                        .openingPoints(List.of())
                        .closingPoints(List.of())
                        .allPoints(List.of())
                        .build())
                .build();
    }

    private StockChartDTO.SearchItemDTO searchItem(StockChartBO.SearchItem value) {
        return StockChartDTO.SearchItemDTO.builder()
                .targetType(value.getTargetType())
                .symbol(value.getSymbol())
                .name(value.getName())
                .build();
    }

    private StockChartDTO.KlineBarDTO bar(StockChartBO.Bar value) {
        Long timestamp = value.getTimestamp();
        if (timestamp == null && value.getTradeDate() != null) {
            timestamp = value.getTradeDate().atTime(15, 0).atZone(SHANGHAI).toInstant().toEpochMilli();
        }
        return StockChartDTO.KlineBarDTO.builder()
                .timestamp(timestamp)
                .tradeDate(value.getTradeDate())
                .open(value.getOpen())
                .high(value.getHigh())
                .low(value.getLow())
                .close(value.getClose())
                .volume(value.getVolume())
                .turnover(value.getTurnover())
                .turnoverRate(value.getTurnoverRate())
                .volumeRatio(value.getVolumeRatio())
                .build();
    }

    private StockChartDTO.FundFlowPointDTO fundFlowPoint(StockChartBO.FundFlowPoint value) {
        return StockChartDTO.FundFlowPointDTO.builder()
                .tradeDate(value.getTradeDate())
                .activeNetAmountWan(value.getActiveNetAmountWan())
                .activeBuyRatio(value.getActiveBuyRatio())
                .activeSellRatio(value.getActiveSellRatio())
                .amountWan(value.getAmountWan())
                .changePct(value.getChangePct())
                .build();
    }

    private StockChartDTO.IntradayPointDTO intradayPoint(StockChartBO.IntradayPoint value) {
        return StockChartDTO.IntradayPointDTO.builder()
                .timestamp(value.getTimestamp())
                .tradeDate(value.getTradeDate())
                .timeLabel(value.getTimeLabel())
                .price(value.getPrice())
                .open(value.getOpen())
                .avgPrice(value.getAvgPrice())
                .volume(value.getVolume())
                .turnover(value.getTurnover())
                .turnoverRate(value.getTurnoverRate())
                .build();
    }

    private StockChartDTO.MinuteBarsDTO minuteBars(Map<String, List<StockChartDTO.KlineBarDTO>> bars) {
        return StockChartDTO.MinuteBarsDTO.builder()
                .oneMinute(bars.getOrDefault("1m", List.of()))
                .fiveMinute(bars.getOrDefault("5m", List.of()))
                .fifteenMinute(bars.getOrDefault("15m", List.of()))
                .thirtyMinute(bars.getOrDefault("30m", List.of()))
                .sixtyMinute(bars.getOrDefault("60m", List.of()))
                .build();
    }

    private StockChartDTO.MinuteBarsDTO emptyMinuteBars() {
        return StockChartDTO.MinuteBarsDTO.builder()
                .oneMinute(List.of())
                .fiveMinute(List.of())
                .fifteenMinute(List.of())
                .thirtyMinute(List.of())
                .sixtyMinute(List.of())
                .build();
    }

    private String normalizePeriod(String period) {
        String value = period == null || period.isBlank() ? "1d" : period.trim().toLowerCase();
        return switch (value) {
            case "1", "1min", "1m" -> "1m";
            case "5", "5min", "5m" -> "5m";
            case "15", "15min", "15m" -> "15m";
            case "30", "30min", "30m" -> "30m";
            case "60", "60min", "60m" -> "60m";
            case "120", "120min", "120m" -> "120m";
            case "day", "daily", "1d" -> "1d";
            default -> "1d";
        };
    }

    private int periodMinutes(String period) {
        String normalized = normalizePeriod(period);
        return switch (normalized) {
            case "1m" -> 1;
            case "5m" -> 5;
            case "15m" -> 15;
            case "30m" -> 30;
            case "60m" -> 60;
            case "120m" -> 120;
            default -> 1;
        };
    }

    private String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private LocalDate parseDateOrToday(String value) {
        return value == null || value.isBlank() ? LocalDate.now(SHANGHAI) : LocalDate.parse(value);
    }
}
