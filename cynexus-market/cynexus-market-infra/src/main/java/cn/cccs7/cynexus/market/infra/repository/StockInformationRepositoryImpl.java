package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;
import cn.cccs7.cynexus.market.domain.repository.StockInformationRepository;
import cn.cccs7.cynexus.market.infra.client.HttpStockChartF10DataClient;
import cn.cccs7.cynexus.market.infra.client.PostgresStockInformationClient;
import cn.cccs7.cynexus.market.infra.client.PostgresStockInformationContentClient;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class StockInformationRepositoryImpl implements StockInformationRepository {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final PostgresStockInformationClient postgresClient;
    private final HttpStockChartF10DataClient f10Client;
    private final PostgresStockInformationContentClient contentClient;

    public StockInformationRepositoryImpl(PostgresStockInformationClient postgresClient,
                                          HttpStockChartF10DataClient f10Client,
                                          PostgresStockInformationContentClient contentClient) {
        this.postgresClient = postgresClient;
        this.f10Client = f10Client;
        this.contentClient = contentClient;
    }

    @Override
    public StockInformationBO.SourceRecord findSourceRecord(String symbol, String kind, String source, String itemId) {
        StockInformationBO.SourceRecord record = contentClient.findSourceRecord(normalizeCode(symbol), kind, source, itemId);
        if (record != null) record.setKind(kind);
        return record;
    }

    @Override
    public StockInformationBO.Content findContent(StockInformationBO.SourceRecord sourceRecord) {
        return contentClient.findContent(sourceRecord);
    }

    @Override
    public StockInformationBO.Content saveContent(StockInformationBO.Content content) {
        return contentClient.saveContent(content);
    }

    @Override
    public StockInformationBO.Analysis findLatestAnalysis(Long contentId, String contentHash, String schemaVersion, String promptVersion) {
        return contentClient.findLatestAnalysis(contentId, contentHash, schemaVersion, promptVersion);
    }

    @Override
    public StockInformationBO.Analysis saveAnalysis(StockInformationBO.Analysis analysis) {
        return contentClient.saveAnalysis(analysis);
    }

    @Override
    public StockInformationBO.Feed feed(String symbol, int limit) {
        String code = normalizeCode(symbol);
        return StockInformationBO.Feed.builder()
                .symbol(code)
                .queriedAt(OffsetDateTime.now(SHANGHAI))
                .announcements(postgresClient.announcements(code, limit))
                .news(postgresClient.news(code, limit))
                .research(postgresClient.research(code, limit))
                .investorInteractions(postgresClient.investorInteractions(code, limit))
                .roadshows(roadshows(code))
                .build();
    }

    private List<StockInformationBO.Roadshow> roadshows(String symbol) {
        try {
            StockChartBO.F10Roadshows response = f10Client.roadshows(symbol);
            return (response.getItems() == null ? List.<StockChartBO.F10RoadshowItem>of() : response.getItems()).stream()
                    .filter(item -> item.getUrl() != null && !item.getUrl().isBlank())
                    .map(item -> StockInformationBO.Roadshow.builder()
                            .title(item.getTitle())
                            .type(item.getRoadshowType())
                            .startAt(startAt(item.getStartDate(), item.getStartTime()))
                            .source(response.getSource())
                            .url(item.getUrl())
                            .build())
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private OffsetDateTime startAt(String date, String time) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            LocalDate parsedDate = date.matches("\\d{8}")
                    ? LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE)
                    : LocalDate.parse(date);
            LocalTime parsedTime = time == null || time.isBlank() ? LocalTime.MIN : LocalTime.parse(time);
            return parsedDate.atTime(parsedTime).atZone(SHANGHAI).toOffsetDateTime();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String normalizeCode(String symbol) {
        String digits = symbol == null ? "" : symbol.replaceAll("\\D", "");
        return digits.length() >= 6 ? digits.substring(digits.length() - 6) : digits;
    }
}
