package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostgresStockInformationClient {

    private static final ZoneId SHANGHAI = ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID);

    private final JdbcTemplate jdbcTemplate;

    public PostgresStockInformationClient(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<StockInformationBO.Announcement> announcements(String symbol, int limit) {
        return jdbcTemplate.query("""
                        select announcement_id, title, category, publish_date, source, url
                          from a_stock_announcement
                         where code = ?
                           and nullif(trim(url), '') is not null
                         order by publish_date desc nulls last, ingested_at desc
                         limit ?
                        """,
                (row, index) -> StockInformationBO.Announcement.builder()
                        .id(row.getString("announcement_id"))
                        .title(row.getString("title"))
                        .category(row.getString("category"))
                        .publishedAt(dateTime(row, "publish_date"))
                        .source(row.getString("source"))
                        .url(row.getString("url"))
                        .build(),
                symbol, limit);
    }

    public List<StockInformationBO.News> news(String symbol, int limit) {
        return jdbcTemplate.query("""
                        select news_id, title, summary, media_name, published_at, source, url
                          from a_stock_news
                         where code = ?
                           and nullif(trim(url), '') is not null
                         order by published_at desc nulls last, ingested_at desc
                         limit ?
                        """,
                (row, index) -> StockInformationBO.News.builder()
                        .id(row.getString("news_id"))
                        .title(row.getString("title"))
                        .summary(row.getString("summary"))
                        .mediaName(row.getString("media_name"))
                        .publishedAt(offsetDateTime(row, "published_at"))
                        .source(row.getString("source"))
                        .url(row.getString("url"))
                        .build(),
                symbol, limit);
    }

    public List<StockInformationBO.Research> research(String symbol, int limit) {
        return jdbcTemplate.query("""
                        select report_id, title, org_name, author, rating, publish_date, source, pdf_url, url
                          from a_stock_research_report
                         where code = ?
                           and (
                               nullif(trim(pdf_url), '') is not null
                               or nullif(trim(url), '') is not null
                               or (source = 'eastmoney.reportapi' and report_id like 'AP%')
                           )
                         order by publish_date desc nulls last, ingested_at desc
                         limit ?
                        """,
                (row, index) -> StockInformationBO.Research.builder()
                        .id(row.getString("report_id"))
                        .title(row.getString("title"))
                        .organization(row.getString("org_name"))
                        .author(row.getString("author"))
                        .rating(row.getString("rating"))
                        .publishedAt(dateTime(row, "publish_date"))
                        .source(row.getString("source"))
                        .url(researchUrl(row))
                        .build(),
                symbol, limit);
    }

    public List<StockInformationBO.InvestorInteraction> investorInteractions(String symbol, int limit) {
        return jdbcTemplate.query("""
                        select interaction_id, question, answer, answerer, asked_at, source
                          from a_stock_investor_interaction
                         where code = ?
                           and (nullif(trim(question), '') is not null or nullif(trim(answer), '') is not null)
                         order by asked_at desc nulls last, ingested_at desc
                         limit ?
                        """,
                (row, index) -> StockInformationBO.InvestorInteraction.builder()
                        .id(row.getString("interaction_id"))
                        .question(row.getString("question"))
                        .answer(row.getString("answer"))
                        .answerer(row.getString("answerer"))
                        .askedAt(offsetDateTime(row, "asked_at"))
                        .source(row.getString("source"))
                        .build(),
                symbol, limit);
    }

    private static String researchUrl(ResultSet row) throws SQLException {
        String pdfUrl = blankToNull(row.getString("pdf_url"));
        if (pdfUrl != null) {
            return pdfUrl;
        }
        String url = blankToNull(row.getString("url"));
        if (url != null) {
            return url;
        }
        String source = row.getString("source");
        String reportId = row.getString("report_id");
        if ("eastmoney.reportapi".equals(source) && reportId != null && reportId.startsWith("AP")) {
            return "https://data.eastmoney.com/report/zw_stock.jshtml?infocode=" + reportId;
        }
        return null;
    }

    private static OffsetDateTime dateTime(ResultSet row, String column) throws SQLException {
        LocalDate value = row.getObject(column, LocalDate.class);
        return value == null ? null : value.atStartOfDay(SHANGHAI).toOffsetDateTime();
    }

    private static OffsetDateTime offsetDateTime(ResultSet row, String column) throws SQLException {
        return row.getObject(column, OffsetDateTime.class);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
