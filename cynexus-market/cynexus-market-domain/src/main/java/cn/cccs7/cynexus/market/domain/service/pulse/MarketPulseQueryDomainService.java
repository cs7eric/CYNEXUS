package cn.cccs7.cynexus.market.domain.service.pulse;

import cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates the market-pulse read and manual fund-flow use cases.
 *
 * <p>The application layer maps these domain results to API DTOs. This service
 * owns the choices that determine which market data is served, including
 * trading-date fallback, intraday/close mode, heatmap routing, and the
 * post-write missing-data contract.</p>
 */
public final class MarketPulseQueryDomainService {

    private static final String LIVE_CAPTURE = "live_capture";
    private static final String INTRADAY = "intraday";
    private static final String CLOSE = "close";
    private static final String STYLES = "styles";
    private static final String DEFAULT_HEATMAP_KIND = "industries";

    private final MarketPulseQueryRepository repository;

    public MarketPulseQueryDomainService(MarketPulseQueryRepository repository) {
        this.repository = repository;
    }

    public SectorSnapshotResult latestSectorSnapshot(LocalDate requestedTradeDate) {
        LocalDate requested = requestedTradeDate == null ? LocalDate.now() : requestedTradeDate;
        LocalDate tradeDate = repository.findLatestPulseTradeDate(requested)
                .or(() -> repository.findLatestPulseTradeDate(null))
                .orElse(null);
        if (tradeDate == null) {
            return new SectorSnapshotResult(requested, null, false, CLOSE, null, null, null, null,
                    List.of(), "market pulse snapshot is empty");
        }

        MarketPulseCaptureBatchBO batch = repository.findLatestPulseBatch(tradeDate).orElse(null);
        List<MarketPulseSectorDailySnapshotBO> rows = repository.listPulseRowsByTradeDate(tradeDate);
        List<MarketPulseSectorDailySnapshotBO> safeRows = rows == null ? List.of() : List.copyOf(rows);
        return new SectorSnapshotResult(
                requested,
                tradeDate,
                !tradeDate.equals(requested),
                resolveServingMode(tradeDate, batch == null ? null : batch.getSourceKind()),
                batch == null ? null : batch.getSourceKind(),
                batch == null ? null : batch.getSourceName(),
                batch == null ? null : batch.getStatus(),
                batch == null ? null : batch.getFetchedAt(),
                safeRows,
                null);
    }

    public Optional<MarketOverviewBO> manualFundFlow(LocalDate tradingDate) {
        return repository.findManualFundFlow(tradingDate);
    }

    public ManualFundFlowSaveResult saveManualFundFlow(LocalDate tradingDate, MarketOverviewBO value) {
        if (tradingDate == null) {
            throw new IllegalArgumentException("tradingDate is required");
        }
        if (value == null) {
            throw new IllegalArgumentException("manual fund flow value is required");
        }
        repository.upsertManualFundFlow(tradingDate, value);
        return new ManualFundFlowSaveResult(tradingDate, repository.findManualFundFlow(tradingDate).orElse(null));
    }

    public HeatmapQuery queryHeatmap(String kind) {
        String normalizedKind = normalizeHeatmapKind(kind);
        if (STYLES.equals(normalizedKind)) {
            List<StyleSectorSnapshotBO> rows = repository.listLatestStyleSectors();
            return HeatmapQuery.styles(rows == null ? List.of() : rows);
        }
        return HeatmapQuery.persisted(normalizedKind, repository.findLatestHeatmap(normalizedKind).orElse(null));
    }

    private String normalizeHeatmapKind(String kind) {
        return kind == null || kind.isBlank() ? DEFAULT_HEATMAP_KIND : kind.trim().toLowerCase();
    }

    private String resolveServingMode(LocalDate tradeDate, String sourceKind) {
        return tradeDate.equals(LocalDate.now()) && LIVE_CAPTURE.equals(sourceKind) ? INTRADAY : CLOSE;
    }

    public record SectorSnapshotResult(
            LocalDate requestedTradeDate,
            LocalDate tradeDate,
            boolean fallbackTradeDate,
            String servingMode,
            String sourceKind,
            String sourceName,
            String status,
            java.time.OffsetDateTime fetchedAt,
            List<MarketPulseSectorDailySnapshotBO> rows,
            String error) {
    }

    public record ManualFundFlowSaveResult(LocalDate tradingDate, MarketOverviewBO value) {

        public boolean foundAfterSave() {
            return value != null;
        }
    }

    public record HeatmapQuery(String kind, HeatmapSnapshotBO snapshot, List<StyleSectorSnapshotBO> styleRows) {

        public static HeatmapQuery styles(List<StyleSectorSnapshotBO> rows) {
            return new HeatmapQuery(STYLES, null, List.copyOf(rows));
        }

        public static HeatmapQuery persisted(String kind, HeatmapSnapshotBO snapshot) {
            return new HeatmapQuery(kind, snapshot, List.of());
        }

        public boolean isStyles() {
            return STYLES.equals(kind);
        }
    }
}
