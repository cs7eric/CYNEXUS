package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostgresStockInformationContentClient {

    private final JdbcTemplate jdbcTemplate;
    private final SnowflakeIdGenerator idGenerator;

    public PostgresStockInformationContentClient(JdbcTemplate jdbcTemplate, SnowflakeIdGenerator idGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.idGenerator = idGenerator;
    }

    public StockInformationBO.SourceRecord findSourceRecord(String code, String kind, String source, String itemId) {
        return switch (kind) {
            case "announcement" -> one("""
                    select source, code, announcement_id as item_id, title, url, null::text as inline_text
                      from a_stock_announcement where code = ? and source = ? and announcement_id = ?
                    """, code, source, itemId);
            case "news" -> one("""
                    select source, code, news_id as item_id, title, url, null::text as inline_text
                      from a_stock_news where code = ? and source = ? and news_id = ?
                    """, code, source, itemId);
            case "research" -> research(code, source, itemId);
            case "interaction" -> one("""
                    select source, code, interaction_id as item_id, question as title, null::text as url, answer as inline_text
                      from a_stock_investor_interaction where code = ? and source = ? and interaction_id = ?
                    """, code, source, itemId);
            default -> null;
        };
    }

    public StockInformationBO.Content findContent(StockInformationBO.SourceRecord record) {
        return jdbcTemplate.query("""
                        select id, title, source_url, resolved_url, content_type, fetch_status, content_text, content_hash, error_message, extracted_at
                          from a_stock_information_content
                         where item_kind = ? and source = ? and code = ? and item_id = ?
                        """, rs -> rs.next() ? content(rs, record) : null,
                record.getKind(), record.getSource(), record.getCode(), record.getItemId());
    }

    public StockInformationBO.Content saveContent(StockInformationBO.Content value) {
        StockInformationBO.SourceRecord record = value.getSourceRecord();
        return jdbcTemplate.queryForObject("""
                        insert into a_stock_information_content (
                            id, item_kind, source, code, item_id, title, source_url, resolved_url, content_type,
                            fetch_status, content_text, content_hash, error_message, extracted_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                        on conflict (item_kind, source, code, item_id) do update set
                            title = excluded.title, source_url = excluded.source_url, resolved_url = excluded.resolved_url,
                            content_type = excluded.content_type, fetch_status = excluded.fetch_status,
                            content_text = excluded.content_text, content_hash = excluded.content_hash,
                            error_message = excluded.error_message, extracted_at = excluded.extracted_at, updated_at = now()
                        returning id, title, source_url, resolved_url, content_type, fetch_status, content_text, content_hash, error_message, extracted_at
                        """, (rs, index) -> content(rs, record),
                idGenerator.nextId(), record.getKind(), record.getSource(), record.getCode(), record.getItemId(),
                value.getTitle(), value.getSourceUrl(), value.getResolvedUrl(), value.getContentType(), value.getStatus(),
                value.getText(), value.getContentHash(), value.getMessage(), value.getExtractedAt());
    }

    public StockInformationBO.Analysis findLatestAnalysis(Long contentId, String contentHash, String schemaVersion, String promptVersion) {
        return jdbcTemplate.query("""
                        select id, content_id, content_hash, analysis_schema_version, prompt_version, model_code, status, result_json, error_message, analyzed_at
                          from a_stock_information_analysis
                         where content_id = ? and content_hash = ? and analysis_schema_version = ? and prompt_version = ?
                         order by analyzed_at desc nulls last, updated_at desc
                         limit 1
                        """, rs -> rs.next() ? analysis(rs) : null, contentId, contentHash, schemaVersion, promptVersion);
    }

    public StockInformationBO.Analysis saveAnalysis(StockInformationBO.Analysis value) {
        return jdbcTemplate.queryForObject("""
                        insert into a_stock_information_analysis (
                            id, content_id, content_hash, analysis_schema_version, prompt_version, model_code,
                            status, result_json, error_message, analyzed_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                        on conflict (content_id, content_hash, analysis_schema_version, prompt_version, model_code) do update set
                            status = excluded.status, result_json = excluded.result_json, error_message = excluded.error_message,
                            analyzed_at = excluded.analyzed_at, updated_at = now()
                        returning id, content_id, content_hash, analysis_schema_version, prompt_version, model_code, status, result_json, error_message, analyzed_at
                        """, (rs, index) -> analysis(rs),
                idGenerator.nextId(), value.getContentId(), value.getContentHash(), value.getSchemaVersion(), value.getPromptVersion(),
                blank(value.getModelCode()), value.getStatus(), value.getResultJson(), value.getErrorMessage(), value.getAnalyzedAt());
    }

    private StockInformationBO.SourceRecord research(String code, String source, String itemId) {
        return jdbcTemplate.query("""
                        select source, code, report_id as item_id, title,
                               coalesce(nullif(pdf_url, ''), nullif(url, ''),
                                   case when source = 'eastmoney.reportapi' and report_id like 'AP%'
                                       then 'https://data.eastmoney.com/report/zw_stock.jshtml?infocode=' || report_id end) as url,
                               null::text as inline_text
                          from a_stock_research_report
                         where code = ? and source = ? and report_id = ?
                        """, rs -> rs.next() ? sourceRecord(rs) : null, code, source, itemId);
    }

    private StockInformationBO.SourceRecord one(String sql, String code, String source, String itemId) {
        return jdbcTemplate.query(sql, rs -> rs.next() ? sourceRecord(rs) : null, code, source, itemId);
    }

    private static StockInformationBO.SourceRecord sourceRecord(ResultSet rs) throws SQLException {
        return StockInformationBO.SourceRecord.builder()
                .source(rs.getString("source")).code(rs.getString("code")).itemId(rs.getString("item_id"))
                .title(rs.getString("title")).url(rs.getString("url")).inlineText(rs.getString("inline_text")).build();
    }

    private static StockInformationBO.Content content(ResultSet rs, StockInformationBO.SourceRecord record) throws SQLException {
        return StockInformationBO.Content.builder().id(rs.getLong("id")).sourceRecord(record).title(rs.getString("title"))
                .sourceUrl(rs.getString("source_url")).resolvedUrl(rs.getString("resolved_url")).contentType(rs.getString("content_type"))
                .status(rs.getString("fetch_status")).text(rs.getString("content_text")).contentHash(rs.getString("content_hash"))
                .message(rs.getString("error_message")).extractedAt(rs.getObject("extracted_at", OffsetDateTime.class)).build();
    }

    private static StockInformationBO.Analysis analysis(ResultSet rs) throws SQLException {
        return StockInformationBO.Analysis.builder().id(rs.getLong("id")).contentId(rs.getLong("content_id"))
                .contentHash(rs.getString("content_hash")).schemaVersion(rs.getString("analysis_schema_version"))
                .promptVersion(rs.getString("prompt_version")).modelCode(rs.getString("model_code")).status(rs.getString("status"))
                .resultJson(rs.getString("result_json")).errorMessage(rs.getString("error_message"))
                .analyzedAt(rs.getObject("analyzed_at", OffsetDateTime.class)).build();
    }

    private static String blank(String value) {
        return Optional.ofNullable(value).orElse("");
    }
}
