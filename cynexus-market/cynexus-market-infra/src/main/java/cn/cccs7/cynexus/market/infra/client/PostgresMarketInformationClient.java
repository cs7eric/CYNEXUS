package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.market.domain.entity.MarketInformationBO;
import cn.cccs7.cynexus.market.domain.information.MarketInformationClusterer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PostgresMarketInformationClient {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final JdbcTemplate jdbcTemplate;
    private final MarketInformationClusterer clusterer;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    public PostgresMarketInformationClient(JdbcTemplate jdbcTemplate,
                                           MarketInformationClusterer clusterer,
                                           PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.clusterer = clusterer;
        this.objectMapper = new ObjectMapper();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public List<MarketInformationBO.News> news(int page, int pageSize, String impactIndustryCode, String analysisStatus) {
        long offset = (long) (page - 1) * pageSize;
        return jdbcTemplate.query("""
                with information_rows as (
                     select news_id, title, summary, source, published_at, url,
                            nullif(trim(code), '') as subject_key,
                           case lower(coalesce(raw_payload ->> 'category', ''))
                               when 'policy' then 'Policy'
                               when 'macro' then 'Macro'
                               when 'regulation' then 'Regulation'
                               when 'industry' then 'Industry'
                               else case
                                   when source in ('gov.cn.policy', 'mof.policy') then 'Policy'
                                   when source in ('pboc.news', 'stats.gov.cn.release', 'xinhua.finance', 'cctv.finance') then 'Macro'
                                   when source in ('csrc.news', 'szse.disclosure') then 'Regulation'
                                   when source in ('ndrc.news', 'caam.industry', 'caam.policy', 'chinania.news', 'chinania.stats', 'chinania.policy') then 'Industry'
                                   else 'Market Wire'
                               end
                           end as category,
                           'news'::text as kind,
                            coalesce(nullif(raw_payload ->> 'sec_name', ''), nullif(raw_payload ->> 'issuer', '')) as issuer,
                           ingested_at
                      from a_stock_news
                     where source in (
                           'cls.telegraph', 'eastmoney.7x24', 'gov.cn.policy', 'pboc.news',
                           'stats.gov.cn.release', 'mof.policy', 'ndrc.news', 'csrc.news',
                           'szse.disclosure', 'xinhua.finance', 'cctv.finance', 'caam.industry',
                           'caam.policy', 'chinania.news', 'chinania.stats', 'chinania.policy'
                     )
                       and nullif(trim(title), '') is not null
                    union all
                     select announcement_id, title, null::text, source,
                            publish_date::timestamp at time zone 'Asia/Shanghai', url,
                            nullif(trim(code), ''),
                           case source when 'cninfo.regulation' then 'Regulation' else 'Policy' end,
                           'announcement'::text,
                           nullif(raw_payload ->> 'sec_name', ''),
                           ingested_at
                      from a_stock_announcement
                     where source in ('cninfo.market', 'cninfo.regulation')
                       and nullif(trim(title), '') is not null
                ), ranked as (
                     select news_id, title, summary, source, published_at, url, subject_key, category, kind, issuer,
                           row_number() over (
                               partition by source
                               order by published_at desc nulls last, ingested_at desc
                           ) as source_rank,
                           case source
                               when 'cls.telegraph' then 1
                               when 'eastmoney.7x24' then 2
                               when 'gov.cn.policy' then 3
                               when 'pboc.news' then 4
                               when 'stats.gov.cn.release' then 5
                               when 'mof.policy' then 6
                               when 'ndrc.news' then 7
                               when 'csrc.news' then 8
                               when 'szse.disclosure' then 9
                               when 'xinhua.finance' then 10
                               when 'cctv.finance' then 11
                               when 'caam.industry' then 12
                               when 'caam.policy' then 13
                               when 'chinania.news' then 14
                               when 'chinania.stats' then 15
                               when 'chinania.policy' then 16
                               when 'cninfo.market' then 17
                               when 'cninfo.regulation' then 18
                               else 99
                           end as source_order
                      from information_rows
                )
                 select ranked.news_id, ranked.title, ranked.summary, ranked.source, ranked.published_at, ranked.url,
                        ranked.subject_key,
                       ranked.category, ranked.kind, ranked.issuer, member.cluster_id,
                       coalesce(task.task_status, analysis.analysis_status, 'NOT_REQUESTED') as analysis_status,
                       analysis.analysis_json ->> 'materiality' as materiality,
                       analysis.analysis_json ->> 'marketDirection' as market_direction
                  from ranked
                  left join market_information_event_member member
                    on member.source = ranked.source and member.source_record_id = ranked.news_id
                  left join market_information_event_cluster cluster on cluster.id = member.cluster_id
                  left join lateral (
                      select analysis_status, analysis_json
                        from cynexus_appl_ai.market_information_event_analysis value
                       where value.cluster_id = member.cluster_id
                         and value.cluster_content_hash = cluster.content_hash
                         and value.schema_version = 'v2'
                         and value.prompt_version = (select prompt.prompt_version from cynexus_appl_ai.ai_usage_bindings binding join cynexus_appl_ai.ai_prompt prompt on prompt.id = binding.prompt_id where binding.capability = 'market_information_event_analysis' and binding.deleted_at is null and binding.is_enabled and prompt.deleted_at is null limit 1)
                       order by value.created_at desc limit 1
                  ) analysis on true
                  left join lateral (
                      select task_status
                        from cynexus_appl_ai.market_information_ai_task value
                       where value.cluster_id = member.cluster_id
                         and value.task_type = 'EVENT_ANALYSIS'
                         and value.cluster_content_hash = cluster.content_hash
                         and value.task_status in ('PENDING', 'RUNNING', 'FAILED')
                       order by value.created_at desc limit 1
                   ) task on true
                 where (cast(? as text) is null or exists (
                     with latest as (select max(snapshot_date) as snapshot_date from ths_industry_constituent_snapshot where deleted_at is null)
                     select 1
                       from tdx_ths_industry_mapping map
                      where map.deleted_at is null and map.tdx_code6 = ?
                        and (exists (select 1 from ths_industry_constituent_snapshot c cross join latest
                                      where c.deleted_at is null and c.snapshot_date = latest.snapshot_date
                                        and c.stock_code = ranked.subject_key and c.industry_code = map.ths_code)
                             or (length(coalesce(map.tdx_name, '')) >= 2
                                 and (ranked.title || ' ' || coalesce(ranked.summary, '')) like '%' || map.tdx_name || '%')
                             or exists (select 1 from (values
                                    ('880390','汽车'), ('880390','新能源汽车'), ('880390','节能汽车'), ('880390','车船税'),
                                    ('880456','环保'), ('880456','生态环境'), ('880456','碳达峰'), ('880456','碳中和'), ('880456','水体'), ('880456','污染治理'),
                                    ('880324','有色'), ('880324','铜'), ('880324','铝'), ('880324','钨'), ('880324','钼'), ('880324','矿山'),
                                    ('880471','人民银行'), ('880471','货币政策'), ('880471','降准'), ('880471','降息'), ('880471','贷款'), ('880471','利率'),
                                    ('880472','证券'), ('880472','资本市场'), ('880472','证监会'), ('880472','股票'), ('880472','上市公司'),
                                    ('880491','半导体'), ('880491','芯片'), ('880491','集成电路'),
                                    ('880335','化工'), ('880335','化学品'), ('880301','煤炭'), ('880310','石油'), ('880318','钢铁'),
                                    ('880476','建筑'), ('880476','基建'), ('880446','电气设备'), ('880446','光伏'), ('880446','风电'),
                                    ('880406','零售'), ('880418','传媒'), ('880418','游戏'), ('880400','医药'), ('880398','医疗'),
                                    ('880372','食品'), ('880380','白酒'), ('880387','家电'), ('880490','通信'), ('880493','软件'), ('880494','互联网')
                                  ) keyword_map(tdx_code6, keyword)
                                  where keyword_map.tdx_code6 = map.tdx_code6
                                    and (ranked.title || ' ' || coalesce(ranked.summary, '')) like '%' || keyword_map.keyword || '%'))
                 ))
                   and (cast(? as text) is null or coalesce(task.task_status, analysis.analysis_status, 'NOT_REQUESTED') = cast(? as text))
                 order by source_rank, source_order, published_at desc nulls last
                 limit ? offset ?
                """, (rs, n) -> {
                    List<MarketInformationBO.ImpactIndustry> impacts = impactIndustries(rs.getString("source"), rs.getString("news_id"),
                            rs.getString("subject_key"), rs.getString("title"), rs.getString("summary"));
                    return MarketInformationBO.News.builder().id(rs.getString("news_id")).title(rs.getString("title"))
                .summary(rs.getString("summary")).source(rs.getString("source")).category(rs.getString("category"))
                .kind(rs.getString("kind")).issuer(rs.getString("issuer"))
                .labels(informationLabels(rs.getString("source"), rs.getString("issuer")))
                .publishedAt(offset(rs, "published_at"))
                .url(rs.getString("url")).clusterId(rs.getObject("cluster_id", Long.class))
                .analysisStatus(rs.getString("analysis_status")).materiality(rs.getString("materiality"))
                .marketDirection(rs.getString("market_direction")).impactIndustries(impacts)
                .impactStatus(impactStatus(impacts)).build();
                }, impactIndustryCode, impactIndustryCode, analysisStatus, analysisStatus, pageSize, offset);
    }

    public long newsCount(String impactIndustryCode, String analysisStatus) {
        Long value = jdbcTemplate.queryForObject("""
                with rows as (
                    select source, news_id as source_record_id, nullif(trim(code), '') as subject_key, title, summary
                      from a_stock_news
                     where source in ('cls.telegraph', 'eastmoney.7x24', 'gov.cn.policy', 'pboc.news', 'stats.gov.cn.release', 'mof.policy', 'ndrc.news', 'csrc.news', 'szse.disclosure', 'xinhua.finance', 'cctv.finance', 'caam.industry', 'caam.policy', 'chinania.news', 'chinania.stats', 'chinania.policy')
                       and nullif(trim(title), '') is not null
                    union all
                    select source, announcement_id, nullif(trim(code), ''), title, null
                      from a_stock_announcement
                     where source in ('cninfo.market', 'cninfo.regulation') and nullif(trim(title), '') is not null
                ), latest as (select max(snapshot_date) as snapshot_date from ths_industry_constituent_snapshot where deleted_at is null), clustered as (
                    select r.*, member.cluster_id, cluster.content_hash,
                           coalesce(task.task_status, analysis.analysis_status, 'NOT_REQUESTED') as analysis_status
                      from rows r
                      left join market_information_event_member member on member.source = r.source and member.source_record_id = r.source_record_id
                      left join market_information_event_cluster cluster on cluster.id = member.cluster_id
                      left join lateral (
                          select analysis_status from cynexus_appl_ai.market_information_event_analysis value
                           where value.cluster_id = member.cluster_id and value.cluster_content_hash = cluster.content_hash
                             and value.schema_version = 'v2' and value.prompt_version = (select prompt.prompt_version from cynexus_appl_ai.ai_usage_bindings binding join cynexus_appl_ai.ai_prompt prompt on prompt.id = binding.prompt_id where binding.capability = 'market_information_event_analysis' and binding.deleted_at is null and binding.is_enabled and prompt.deleted_at is null limit 1)
                           order by value.created_at desc limit 1
                      ) analysis on true
                      left join lateral (
                          select task_status from cynexus_appl_ai.market_information_ai_task value
                           where value.cluster_id = member.cluster_id and value.task_type = 'EVENT_ANALYSIS'
                             and value.cluster_content_hash = cluster.content_hash
                             and value.task_status in ('PENDING', 'RUNNING', 'FAILED')
                           order by value.created_at desc limit 1
                      ) task on true
                )
                select count(*) from clustered r cross join latest
                 where (cast(? as text) is null or exists (
                     select 1 from tdx_ths_industry_mapping map
                      where map.deleted_at is null and map.tdx_code6 = ?
                        and (exists (select 1 from ths_industry_constituent_snapshot c
                                      where c.deleted_at is null and c.snapshot_date = latest.snapshot_date
                                        and c.stock_code = r.subject_key and c.industry_code = map.ths_code)
                             or (length(coalesce(map.tdx_name, '')) >= 2 and (r.title || ' ' || coalesce(r.summary, '')) like '%' || map.tdx_name || '%')
                             or exists (select 1 from (values
                                    ('880390','汽车'), ('880390','新能源汽车'), ('880390','节能汽车'), ('880390','车船税'),
                                    ('880456','环保'), ('880456','生态环境'), ('880456','碳达峰'), ('880456','碳中和'), ('880456','水体'), ('880456','污染治理'),
                                    ('880324','有色'), ('880324','铜'), ('880324','铝'), ('880324','钨'), ('880324','钼'), ('880324','矿山'),
                                    ('880471','人民银行'), ('880471','货币政策'), ('880471','降准'), ('880471','降息'), ('880471','贷款'), ('880471','利率'),
                                    ('880472','证券'), ('880472','资本市场'), ('880472','证监会'), ('880472','股票'), ('880472','上市公司'),
                                    ('880491','半导体'), ('880491','芯片'), ('880491','集成电路'),
                                    ('880335','化工'), ('880335','化学品'), ('880301','煤炭'), ('880310','石油'), ('880318','钢铁'),
                                    ('880476','建筑'), ('880476','基建'), ('880446','电气设备'), ('880446','光伏'), ('880446','风电'),
                                    ('880406','零售'), ('880418','传媒'), ('880418','游戏'), ('880400','医药'), ('880398','医疗'),
                                    ('880372','食品'), ('880380','白酒'), ('880387','家电'), ('880490','通信'), ('880493','软件'), ('880494','互联网')
                                  ) keyword_map(tdx_code6, keyword)
                                  where keyword_map.tdx_code6 = map.tdx_code6
                                    and (r.title || ' ' || coalesce(r.summary, '')) like '%' || keyword_map.keyword || '%'))
                 ))
                   and (cast(? as text) is null or r.analysis_status = cast(? as text))
                """, Long.class, impactIndustryCode, impactIndustryCode, analysisStatus, analysisStatus);
        return value == null ? 0 : value;
    }

    public List<MarketInformationBO.ImpactIndustry> impactIndustries() {
        return jdbcTemplate.query("""
                select distinct on (tdx_code6) tdx_code6, tdx_name, ths_code, ths_name, confidence
                  from tdx_ths_industry_mapping
                 where deleted_at is null
                 order by tdx_code6, priority asc, confidence desc
        """, (rs, n) -> MarketInformationBO.ImpactIndustry.builder()
                .tdxCode(rs.getString("tdx_code6")).tdxName(rs.getString("tdx_name"))
                .thsCode(rs.getString("ths_code")).thsName(rs.getString("ths_name"))
                .status("Direct").source("TDX56 / THS90 mapping").confidence(rs.getBigDecimal("confidence")).build());
    }

    public List<MarketInformationBO.PolicyAnnouncement> policyAnnouncements(int limit) {
        return jdbcTemplate.query("""
                select announcement_id, title, category, publish_date, url, raw_payload ->> 'sec_name' as issuer
                  from a_stock_announcement
                 where source in ('cninfo.market', 'cninfo.regulation') and nullif(trim(title), '') is not null
                 order by publish_date desc nulls last, ingested_at desc
                 limit ?
                """, (rs, n) -> MarketInformationBO.PolicyAnnouncement.builder()
                .id(rs.getString("announcement_id")).title(rs.getString("title")).category(rs.getString("category"))
                .publishedDate(rs.getObject("publish_date", LocalDate.class)).url(rs.getString("url"))
                .issuer(rs.getString("issuer")).build(), limit);
    }

    private List<String> informationLabels(String source, String issuer) {
        List<String> labels = new ArrayList<>();
        if (source == null) return labels;
        if (source.equals("cls.telegraph") || source.equals("eastmoney.7x24")) labels.add("Market Wire");
        else if (source.equals("szse.disclosure")) labels.add("Exchange");
        else if (source.equals("xinhua.finance") || source.equals("cctv.finance")) labels.add("Financial Media");
        else if (source.startsWith("caam.") || source.startsWith("chinania.")) labels.add("Industry Association");
        else if (source.equals("cninfo.regulation")) labels.add("Regulatory Filing");
        else if (source.equals("cninfo.market")) labels.add("Market Announcement");
        else labels.add("Official");
        if (issuer != null && !issuer.isBlank()) labels.add(issuer);
        return List.copyOf(labels);
    }

    private List<MarketInformationBO.ImpactIndustry> impactIndustries(String source, String sourceRecordId,
                                                                       String subjectKey, String title, String summary) {
        List<MarketInformationBO.ImpactIndustry> direct = jdbcTemplate.query("""
                with latest as (select max(snapshot_date) as snapshot_date from ths_industry_constituent_snapshot where deleted_at is null)
                select distinct on (map.tdx_code6) map.tdx_code6, map.tdx_name, map.ths_code, map.ths_name, map.confidence
                  from ths_industry_constituent_snapshot constituent
                  cross join latest
                  join tdx_ths_industry_mapping map on map.ths_code = constituent.industry_code and map.deleted_at is null
                 where constituent.deleted_at is null and constituent.snapshot_date = latest.snapshot_date and constituent.stock_code = ?
                 order by map.tdx_code6, map.priority asc, map.confidence desc
                """, (rs, n) -> MarketInformationBO.ImpactIndustry.builder()
                .tdxCode(rs.getString("tdx_code6")).tdxName(rs.getString("tdx_name"))
                .thsCode(rs.getString("ths_code")).thsName(rs.getString("ths_name"))
                .status("Direct").source("stock_code -> THS90 -> TDX56").confidence(rs.getBigDecimal("confidence"))
                .relatedStocks(relatedStocks(rs.getString("ths_code"), subjectKey)).build(), subjectKey == null ? "" : subjectKey);
        if (!direct.isEmpty()) return direct;
        if (subjectKey == null || subjectKey.isBlank()) {
            List<String> matchedCodes = jdbcTemplate.query("""
                    select code from sec_stock_basic
                     where nullif(trim(code), '') is not null and length(coalesce(name, '')) >= 2
                       and ? like '%' || name || '%'
                     order by length(name) desc
                     limit 1
                    """, (rs, n) -> rs.getString(1), title == null ? "" : title);
            if (!matchedCodes.isEmpty()) {
                List<MarketInformationBO.ImpactIndustry> issuerImpacts = impactIndustries(source, sourceRecordId,
                        matchedCodes.get(0), title, summary);
                if (!issuerImpacts.isEmpty()) return issuerImpacts;
            }
        }
        if (title == null || title.isBlank()) return List.of();
        return jdbcTemplate.query("""
                select distinct on (tdx_code6) tdx_code6, tdx_name, ths_code, ths_name, confidence
                  from tdx_ths_industry_mapping
                 where deleted_at is null and (
                   (length(coalesce(tdx_name, '')) >= 2 and (? || ' ' || coalesce(?, '')) like '%' || tdx_name || '%')
                   or exists (select 1 from (values
                          ('880390','汽车'), ('880390','新能源汽车'), ('880390','节能汽车'), ('880390','车船税'),
                          ('880456','环保'), ('880456','生态环境'), ('880456','碳达峰'), ('880456','碳中和'), ('880456','水体'), ('880456','污染治理'),
                          ('880324','有色'), ('880324','铜'), ('880324','铝'), ('880324','钨'), ('880324','钼'), ('880324','矿山'),
                          ('880471','人民银行'), ('880471','货币政策'), ('880471','降准'), ('880471','降息'), ('880471','贷款'), ('880471','利率'),
                          ('880472','证券'), ('880472','资本市场'), ('880472','证监会'), ('880472','股票'), ('880472','上市公司'),
                          ('880491','半导体'), ('880491','芯片'), ('880491','集成电路'),
                          ('880335','化工'), ('880335','化学品'), ('880301','煤炭'), ('880310','石油'), ('880318','钢铁'),
                          ('880476','建筑'), ('880476','基建'), ('880446','电气设备'), ('880446','光伏'), ('880446','风电'),
                          ('880406','零售'), ('880418','传媒'), ('880418','游戏'), ('880400','医药'), ('880398','医疗'),
                          ('880372','食品'), ('880380','白酒'), ('880387','家电'), ('880490','通信'), ('880493','软件'), ('880494','互联网')
                        ) keyword_map(tdx_code6, keyword)
                        where keyword_map.tdx_code6 = tdx_ths_industry_mapping.tdx_code6
                          and (? || ' ' || coalesce(?, '')) like '%' || keyword_map.keyword || '%')
                 )
                 order by tdx_code6, priority asc, confidence desc
                """, (rs, n) -> MarketInformationBO.ImpactIndustry.builder()
                .tdxCode(rs.getString("tdx_code6")).tdxName(rs.getString("tdx_name"))
                .thsCode(rs.getString("ths_code")).thsName(rs.getString("ths_name"))
                .status("Inferred").source("title keyword").confidence(rs.getBigDecimal("confidence"))
                .relatedStocks(relatedStocks(rs.getString("ths_code"), null)).build(), title, summary, title, summary);
    }

    private List<MarketInformationBO.RelatedStock> relatedStocks(String thsCode, String subjectKey) {
        if (subjectKey != null && !subjectKey.isBlank()) {
            List<MarketInformationBO.RelatedStock> direct = jdbcTemplate.query("""
                    select code, name from sec_stock_basic where code = ? and nullif(trim(name), '') is not null limit 1
                    """, (rs, n) -> new MarketInformationBO.RelatedStock(rs.getString("code"), rs.getString("name")), subjectKey);
            if (!direct.isEmpty()) return direct;
        }
        if (thsCode == null || thsCode.isBlank()) return List.of();
        return jdbcTemplate.query("""
                with latest as (select max(snapshot_date) as snapshot_date from ths_industry_constituent_snapshot where deleted_at is null)
                select constituent.stock_code, coalesce(nullif(trim(constituent.stock_name), ''), basic.name) as stock_name
                  from ths_industry_constituent_snapshot constituent
                  cross join latest
                  left join sec_stock_basic basic on basic.code = constituent.stock_code
                 where constituent.deleted_at is null and constituent.snapshot_date = latest.snapshot_date and constituent.industry_code = ?
                 order by constituent.rank nulls last, constituent.stock_code
                 limit 5
                """, (rs, n) -> new MarketInformationBO.RelatedStock(rs.getString("stock_code"), rs.getString("stock_name")), thsCode);
    }

    private String rawSubjectKey(String source, String sourceRecordId) {
        List<String> values = jdbcTemplate.query("""
                select nullif(trim(code), '') from a_stock_news where source = ? and news_id = ?
                union all
                select nullif(trim(code), '') from a_stock_announcement where source = ? and announcement_id = ?
                limit 1
                """, (rs, n) -> rs.getString(1), source, sourceRecordId, source, sourceRecordId);
        return values.isEmpty() ? null : values.get(0);
    }

    private String impactStatus(List<MarketInformationBO.ImpactIndustry> impacts) {
        if (impacts == null || impacts.isEmpty()) return "Unmapped";
        return impacts.stream().anyMatch(item -> "Direct".equals(item.getStatus())) ? "Direct" : "Inferred";
    }

    private List<MarketInformationBO.ImpactIndustry> distinctImpacts(List<MarketInformationBO.ImpactIndustry> impacts) {
        Map<String, MarketInformationBO.ImpactIndustry> values = new LinkedHashMap<>();
        for (MarketInformationBO.ImpactIndustry item : impacts) values.putIfAbsent(item.getTdxCode() + ":" + item.getStatus(), item);
        return List.copyOf(values.values());
    }

    public List<MarketInformationBO.Signal> signals(int limit) {
        return jdbcTemplate.query("""
                select code, name, pool, reason, pct, seal_amount, snapshot_date
                  from a_stock_limit_pool_snapshot
                 where snapshot_date = (select max(snapshot_date) from a_stock_limit_pool_snapshot)
                   and pool in ('zt', 'zb', 'dt')
                 order by case pool when 'zt' then 0 when 'zb' then 1 else 2 end, seal_amount desc nulls last
                 limit ?
                """, (rs, n) -> MarketInformationBO.Signal.builder().code(rs.getString("code")).name(rs.getString("name"))
                .pool(rs.getString("pool")).reason(rs.getString("reason")).changePct(rs.getBigDecimal("pct"))
                .sealAmount(rs.getBigDecimal("seal_amount")).snapshotDate(rs.getObject("snapshot_date", LocalDate.class)).build(), limit);
    }

    public List<MarketInformationBO.HotStock> hotStocks(int limit) {
        return jdbcTemplate.query("""
                select code, name, rank_no, heat, pct, snapshot_date
                  from a_stock_hot_rank_snapshot
                 where source = 'eastmoney.app.hot_rank'
                   and list_type = 'current'
                   and snapshot_date = (select max(snapshot_date) from a_stock_hot_rank_snapshot where source = 'eastmoney.app.hot_rank' and list_type = 'current')
                 order by rank_no asc limit ?
                """, (rs, n) -> MarketInformationBO.HotStock.builder().code(rs.getString("code")).name(rs.getString("name"))
                .rank(rs.getInt("rank_no")).heat(rs.getBigDecimal("heat")).pct(rs.getBigDecimal("pct"))
                .snapshotDate(rs.getObject("snapshot_date", LocalDate.class)).build(), limit);
    }

    public List<MarketInformationBO.HotStock> leaders(int limit) {
        return hotStocks(limit);
    }

    public List<MarketInformationBO.LeaderEvent> leaderEvents(List<MarketInformationBO.HotStock> leaders, List<MarketInformationBO.DragonTiger> dragonTiger, int limit) {
        Map<String, String> names = new LinkedHashMap<>();
        leaders.stream().limit(20).filter(item -> item.getName() != null && item.getName().length() >= 2).forEach(item -> names.put(item.getCode(), item.getName()));
        dragonTiger.stream().limit(10).filter(item -> item.getName() != null && item.getName().length() >= 2).forEach(item -> names.putIfAbsent(item.getCode(), item.getName()));
        if (names.isEmpty()) return List.of();
        List<MarketInformationBO.News> candidates = jdbcTemplate.query("""
                select news_id, title, summary, source, published_at, url
                  from a_stock_news
                 where source in ('cls.telegraph', 'eastmoney.7x24')
                 order by published_at desc nulls last, ingested_at desc
                 limit ?
                """, (rs, n) -> MarketInformationBO.News.builder().id(rs.getString("news_id")).title(rs.getString("title"))
                .summary(rs.getString("summary")).source(rs.getString("source")).publishedAt(offset(rs, "published_at"))
                .url(rs.getString("url")).build(), Math.min(limit * 8, 400));
        List<MarketInformationBO.LeaderEvent> events = new ArrayList<>();
        for (MarketInformationBO.News item : candidates) {
            String haystack = ((item.getTitle() == null ? "" : item.getTitle()) + " " + (item.getSummary() == null ? "" : item.getSummary())).toLowerCase();
            for (Map.Entry<String, String> entry : names.entrySet()) {
                if (haystack.contains(entry.getValue().toLowerCase())) {
                    events.add(MarketInformationBO.LeaderEvent.builder().code(entry.getKey()).name(entry.getValue()).title(item.getTitle())
                            .summary(item.getSummary()).source(item.getSource()).publishedAt(item.getPublishedAt()).url(item.getUrl()).build());
                    break;
                }
            }
            if (events.size() >= limit) break;
        }
        return events;
    }

    public List<MarketInformationBO.DragonTiger> dragonTiger(int limit) {
        return jdbcTemplate.query("""
                select code, name, reason, change_pct, net_buy_wan, trade_date
                  from a_stock_dragon_tiger_daily
                 where trade_date = (select max(trade_date) from a_stock_dragon_tiger_daily)
                 order by net_buy_wan desc nulls last limit ?
                """, (rs, n) -> MarketInformationBO.DragonTiger.builder().code(rs.getString("code")).name(rs.getString("name"))
                .reason(rs.getString("reason")).changePct(rs.getBigDecimal("change_pct")).netBuyWan(rs.getBigDecimal("net_buy_wan"))
                .tradeDate(rs.getObject("trade_date", LocalDate.class)).build(), limit);
    }

    public MarketInformationBO.Brief latestBrief() {
        List<MarketInformationBO.Brief> rows = jdbcTemplate.query("""
                select brief_date, brief_status, provider_code, model_code, fallback_used,
                       brief_json::text as content_json, updated_at
                  from cynexus_appl_ai.market_information_daily_brief
                 where brief_status = 'SUCCEEDED'
                 order by brief_date desc, created_at desc
                 limit 1
                """, (rs, n) -> MarketInformationBO.Brief.builder()
                .briefDate(rs.getObject("brief_date", LocalDate.class)).status(rs.getString("brief_status"))
                .providerCode(rs.getString("provider_code")).modelCode(rs.getString("model_code"))
                .fallbackUsed(rs.getBoolean("fallback_used")).contentJson(rs.getString("content_json"))
                .updatedAt(offset(rs, "updated_at")).build());
        return rows.isEmpty() ? null : rows.get(0);
    }

    public MarketInformationBO.EventDetail eventDetail(long clusterId) {
        List<MarketInformationBO.EventDetail> rows = jdbcTemplate.query("""
                select cluster.id, cluster.display_title, cluster.category, cluster.entities::text as entities,
                       cluster.priority_score, cluster.high_value, cluster.first_published_at, cluster.last_published_at,
                       cluster.content_hash, coalesce(task.task_status, analysis.analysis_status, 'NOT_REQUESTED') as analysis_status,
                       analysis.provider_code, analysis.model_code, analysis.fallback_used,
                       analysis.analysis_json::text as analysis_json
                  from market_information_event_cluster cluster
                  left join lateral (
                      select * from cynexus_appl_ai.market_information_event_analysis value
                       where value.cluster_id = cluster.id and value.cluster_content_hash = cluster.content_hash
                         and value.schema_version = 'v2' and value.prompt_version = (select prompt.prompt_version from cynexus_appl_ai.ai_usage_bindings binding join cynexus_appl_ai.ai_prompt prompt on prompt.id = binding.prompt_id where binding.capability = 'market_information_event_analysis' and binding.deleted_at is null and binding.is_enabled and prompt.deleted_at is null limit 1)
                       order by value.created_at desc limit 1
                  ) analysis on true
                  left join lateral (
                      select task_status from cynexus_appl_ai.market_information_ai_task value
                       where value.cluster_id = cluster.id and value.task_type = 'EVENT_ANALYSIS'
                         and value.cluster_content_hash = cluster.content_hash
                         and value.task_status in ('PENDING', 'RUNNING', 'FAILED')
                       order by value.created_at desc limit 1
                  ) task on true
                 where cluster.id = ?
                """, (rs, n) -> MarketInformationBO.EventDetail.builder()
                .clusterId(rs.getLong("id")).title(rs.getString("display_title")).category(rs.getString("category"))
                .entities(readStringList(rs.getString("entities"))).priorityScore(rs.getBigDecimal("priority_score"))
                .highValue(rs.getBoolean("high_value")).firstPublishedAt(offset(rs, "first_published_at"))
                .lastPublishedAt(offset(rs, "last_published_at")).contentHash(rs.getString("content_hash"))
                .analysisStatus(rs.getString("analysis_status")).providerCode(rs.getString("provider_code"))
                .modelCode(rs.getString("model_code")).fallbackUsed(rs.getObject("fallback_used", Boolean.class))
                .analysisJson(rs.getString("analysis_json")).build(), clusterId);
        if (rows.isEmpty()) return null;
        MarketInformationBO.EventDetail detail = rows.get(0);
        detail.setMembers(eventMembers(clusterId));
        List<MarketInformationBO.ImpactIndustry> impacts = new ArrayList<>();
        for (MarketInformationBO.EventMember member : detail.getMembers()) {
            String subjectKey = rawSubjectKey(member.getSource(), member.getSourceRecordId());
            impacts.addAll(impactIndustries(member.getSource(), member.getSourceRecordId(), subjectKey, member.getTitle(), member.getSummary()));
        }
        detail.setImpactIndustries(distinctImpacts(impacts));
        return detail;
    }

    public List<MarketInformationBO.News> analysisStatuses(List<Long> clusterIds) {
        if (clusterIds == null || clusterIds.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(clusterIds.size(), "?"));
        return jdbcTemplate.query("""
                select cluster.id,
                       coalesce(task.task_status, analysis.analysis_status, 'NOT_REQUESTED') as analysis_status,
                       analysis.analysis_json ->> 'materiality' as materiality,
                       analysis.analysis_json ->> 'marketDirection' as market_direction
                  from market_information_event_cluster cluster
                  left join lateral (
                      select analysis_status, analysis_json from cynexus_appl_ai.market_information_event_analysis value
                       where value.cluster_id = cluster.id and value.cluster_content_hash = cluster.content_hash
                         and value.schema_version = 'v2' and value.prompt_version = (select prompt.prompt_version from cynexus_appl_ai.ai_usage_bindings binding join cynexus_appl_ai.ai_prompt prompt on prompt.id = binding.prompt_id where binding.capability = 'market_information_event_analysis' and binding.deleted_at is null and binding.is_enabled and prompt.deleted_at is null limit 1)
                       order by value.created_at desc limit 1
                  ) analysis on true
                  left join lateral (
                      select task_status from cynexus_appl_ai.market_information_ai_task value
                       where value.cluster_id = cluster.id and value.task_type = 'EVENT_ANALYSIS'
                         and value.cluster_content_hash = cluster.content_hash
                         and value.task_status in ('PENDING', 'RUNNING', 'FAILED')
                       order by value.created_at desc limit 1
                  ) task on true
                 where cluster.id in (%s)
                """.formatted(placeholders), (rs, n) -> MarketInformationBO.News.builder()
                .clusterId(rs.getLong("id")).analysisStatus(rs.getString("analysis_status"))
                .materiality(rs.getString("materiality")).marketDirection(rs.getString("market_direction")).build(),
                clusterIds.toArray());
    }

    public boolean requestAnalysis(long clusterId, boolean force, String requestedBy) {
        List<String> hashes = jdbcTemplate.query("select content_hash from market_information_event_cluster where id = ?",
                (rs, n) -> rs.getString(1), clusterId);
        if (hashes.isEmpty()) return false;
        if (force) {
            jdbcTemplate.update("""
                    update cynexus_appl_ai.market_information_ai_task
                       set task_status = 'PENDING', attempt_count = 0, next_attempt_at = now(), last_error = null,
                           requested_by = ?, updated_at = now()
                     where task_type = 'EVENT_ANALYSIS' and cluster_id = ? and task_status in ('PENDING', 'RUNNING')
                    """, requestedBy, clusterId);
        }
        int inserted = jdbcTemplate.update("""
                insert into cynexus_appl_ai.market_information_ai_task
                    (task_type, cluster_id, cluster_content_hash, priority, requested_by)
                select 'EVENT_ANALYSIS', id, content_hash, case when high_value then 100 else 10 end, ?
                  from market_information_event_cluster cluster
                 where id = ?
                   and not exists (
                       select 1 from cynexus_appl_ai.market_information_ai_task task
                        where task.task_type = 'EVENT_ANALYSIS' and task.cluster_id = cluster.id
                          and task.cluster_content_hash = cluster.content_hash
                          and task.task_status in ('PENDING', 'RUNNING')
                   )
                """, requestedBy, clusterId);
        return inserted > 0 || force;
    }

    public int refreshPipeline() {
        List<MarketInformationClusterer.Candidate> candidates = informationCandidates();
        List<MarketInformationClusterer.Cluster> clusters = clusterer.cluster(candidates);
        transactionTemplate.executeWithoutResult(status -> {
            for (MarketInformationClusterer.Candidate candidate : candidates) upsertSummaryContent(candidate);
            for (MarketInformationClusterer.Cluster cluster : clusters) {
                jdbcTemplate.update("""
                        insert into market_information_event_cluster
                            (id, cluster_key, representative_source, representative_record_id, normalized_title,
                             display_title, category, entities, first_published_at, last_published_at, source_count,
                             member_count, priority_score, high_value, content_hash)
                        values (?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), ?, ?, ?, ?, ?, ?, ?)
                        on conflict (cluster_key) do update set
                            representative_source = excluded.representative_source,
                            representative_record_id = excluded.representative_record_id,
                            normalized_title = excluded.normalized_title,
                            display_title = excluded.display_title,
                            category = excluded.category,
                            entities = excluded.entities,
                            first_published_at = least(market_information_event_cluster.first_published_at, excluded.first_published_at),
                            last_published_at = greatest(market_information_event_cluster.last_published_at, excluded.last_published_at),
                            source_count = excluded.source_count,
                            member_count = excluded.member_count,
                            priority_score = excluded.priority_score,
                            high_value = excluded.high_value,
                            content_hash = excluded.content_hash,
                            updated_at = now()
                        """, cluster.id(), cluster.key(), cluster.representativeSource(), cluster.representativeRecordId(),
                        cluster.normalizedTitle(), cluster.displayTitle(), cluster.category(), toJson(cluster.entities()),
                        cluster.firstPublishedAt(), cluster.lastPublishedAt(), cluster.sourceCount(), cluster.members().size(),
                        cluster.priorityScore(), cluster.highValue(), cluster.contentHash());
                for (MarketInformationClusterer.Member member : cluster.members()) {
                    MarketInformationClusterer.Candidate candidate = member.candidate();
                    jdbcTemplate.update("""
                            insert into market_information_event_member
                                (cluster_id, source, source_record_id, published_at, similarity)
                            select id, ?, ?, ?, ? from market_information_event_cluster where cluster_key = ?
                            on conflict (source, source_record_id) do update set
                                cluster_id = excluded.cluster_id,
                                published_at = excluded.published_at,
                                similarity = excluded.similarity
                            """, candidate.source(), candidate.sourceRecordId(), candidate.publishedAt(), member.similarity(), cluster.key());
                }
            }
            jdbcTemplate.update("""
                    delete from market_information_event_member member
                     where not exists (
                         select 1 from a_stock_news news
                          where news.source = member.source and news.news_id = member.source_record_id
                         union all
                         select 1 from a_stock_announcement announcement
                          where announcement.source = member.source and announcement.announcement_id = member.source_record_id
                     )
                    """);
            jdbcTemplate.update("""
                    insert into market_information_pipeline_state
                        (pipeline_key, cursor_value, state_json, last_succeeded_at, updated_at)
                    values ('cluster_refresh', ?, jsonb_build_object('candidateCount', ?, 'clusterCount', ?), now(), now())
                    on conflict (pipeline_key) do update
                       set cursor_value = excluded.cursor_value, state_json = excluded.state_json,
                           last_succeeded_at = now(), failure_message = null, updated_at = now()
                    """, OffsetDateTime.now(SHANGHAI).toString(), candidates.size(), clusters.size());
            jdbcTemplate.update("""
                    insert into cynexus_appl_ai.market_information_ai_task
                        (task_type, cluster_id, cluster_content_hash, priority, requested_by)
                    select 'EVENT_ANALYSIS', cluster.id, cluster.content_hash, round(cluster.priority_score * 10)::int, 'pipeline'
                     from market_information_event_cluster cluster
                     where cluster.high_value
                       and exists (
                           select 1 from market_information_event_member member
                            where member.cluster_id = cluster.id
                       )
                       and not exists (
                           select 1 from cynexus_appl_ai.market_information_event_analysis analysis
                            where analysis.cluster_id = cluster.id and analysis.cluster_content_hash = cluster.content_hash
                              and analysis.schema_version = 'v2' and analysis.prompt_version = (select prompt.prompt_version from cynexus_appl_ai.ai_usage_bindings binding join cynexus_appl_ai.ai_prompt prompt on prompt.id = binding.prompt_id where binding.capability = 'market_information_event_analysis' and binding.deleted_at is null and binding.is_enabled and prompt.deleted_at is null limit 1)
                              and analysis.analysis_status = 'SUCCEEDED'
                       )
                       and not exists (
                           select 1 from cynexus_appl_ai.market_information_ai_task task
                            where task.cluster_id = cluster.id and task.cluster_content_hash = cluster.content_hash
                              and task.task_type = 'EVENT_ANALYSIS'
                       )
                     order by cluster.priority_score desc, cluster.last_published_at desc
                     limit 10
                    """);
        });
        return clusters.size();
    }

    private List<MarketInformationBO.EventMember> eventMembers(long clusterId) {
        return jdbcTemplate.query("""
                with raw as (
                    select source, news_id as source_record_id, title, summary, url, published_at
                      from a_stock_news
                    union all
                    select source, announcement_id, title, null::text, url,
                           publish_date::timestamp at time zone 'Asia/Shanghai'
                      from a_stock_announcement
                )
                select member.source, member.source_record_id, raw.title, raw.summary, raw.url, member.published_at,
                       content.status as content_status, content.content_quality
                  from market_information_event_member member
                  left join raw on raw.source = member.source and raw.source_record_id = member.source_record_id
                  left join lateral (
                      select status, content_quality from market_information_content_cache value
                       where value.source = member.source and value.source_record_id = member.source_record_id
                       order by case value.content_quality when 'full_text' then 0 when 'ocr' then 1 else 2 end,
                                value.updated_at desc limit 1
                  ) content on true
                 where member.cluster_id = ?
                 order by member.published_at desc
                """, (rs, n) -> MarketInformationBO.EventMember.builder()
                .source(rs.getString("source")).sourceRecordId(rs.getString("source_record_id"))
                .title(rs.getString("title")).summary(rs.getString("summary")).url(rs.getString("url"))
                .publishedAt(offset(rs, "published_at")).contentStatus(rs.getString("content_status"))
                .contentQuality(rs.getString("content_quality")).build(), clusterId);
    }

    private List<MarketInformationClusterer.Candidate> informationCandidates() {
        return jdbcTemplate.query("""
                with raw as (
                    select news_id as source_record_id, title, summary, source, published_at, url,
                           nullif(trim(code), '') as subject_key,
                           coalesce(nullif(raw_payload ->> 'sec_name', ''), nullif(raw_payload ->> 'issuer', '')) as issuer,
                           case lower(coalesce(raw_payload ->> 'category', ''))
                               when 'policy' then 'Policy' when 'macro' then 'Macro'
                               when 'regulation' then 'Regulation' when 'industry' then 'Industry'
                               else 'Market Wire' end as category
                      from a_stock_news
                     where source in (
                           'cls.telegraph', 'eastmoney.7x24', 'gov.cn.policy', 'pboc.news', 'stats.gov.cn.release',
                           'mof.policy', 'ndrc.news', 'csrc.news', 'szse.disclosure', 'xinhua.finance', 'cctv.finance',
                           'caam.industry', 'caam.policy', 'chinania.news', 'chinania.stats', 'chinania.policy')
                       and nullif(trim(title), '') is not null and published_at >= now() - interval '30 days'
                    union all
                    select announcement_id, title,
                           concat_ws(' ', nullif(raw_payload ->> 'sec_name', ''), nullif(code, '')) as summary,
                           source,
                           publish_date::timestamp at time zone 'Asia/Shanghai', url,
                           nullif(trim(code), '') as subject_key,
                           nullif(raw_payload ->> 'sec_name', '') as issuer,
                           case source when 'cninfo.regulation' then 'Regulation' else 'Policy' end
                      from a_stock_announcement
                     where source in ('cninfo.market', 'cninfo.regulation') and nullif(trim(title), '') is not null
                       and publish_date >= current_date - 30
                )
                select raw.*, content.content_quality
                  from raw
                  left join lateral (
                      select value.content_quality
                        from market_information_content_cache value
                       where value.source = raw.source and value.source_record_id = raw.source_record_id
                       order by case value.content_quality when 'full_text' then 0 when 'ocr' then 1 else 2 end,
                                value.updated_at desc
                       limit 1
                  ) content on true
                """, (rs, n) -> new MarketInformationClusterer.Candidate(rs.getString("source"),
                rs.getString("source_record_id"), rs.getString("title"), rs.getString("summary"),
                rs.getString("category"), offset(rs, "published_at"), rs.getString("url"),
                rs.getString("subject_key"), rs.getString("issuer"), rs.getString("content_quality")));
    }

    private void upsertSummaryContent(MarketInformationClusterer.Candidate candidate) {
        String content = (candidate.title() + "\n" + (candidate.summary() == null ? "" : candidate.summary())).trim();
        String contentHash = sha256(content);
        long id = Long.parseUnsignedLong(sha256(candidate.identity() + "|" + contentHash).substring(0, 15), 16);
        jdbcTemplate.update("""
                insert into market_information_content_cache
                    (id, source, source_record_id, source_url, title, content_text, content_hash,
                     content_type, content_quality, status, extracted_at)
                values (?, ?, ?, ?, ?, ?, ?, 'text/plain', 'summary_only', 'ready', now())
                on conflict (source, source_record_id, content_hash) do update
                   set source_url = excluded.source_url, title = excluded.title
                """, id, candidate.source(), candidate.sourceRecordId(), candidate.url(), candidate.title(), content, contentHash);
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() { }); }
        catch (JsonProcessingException ignored) { return List.of(); }
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to serialize market information", exception); }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash market information", exception);
        }
    }

    private OffsetDateTime offset(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        if (value != null) return value;
        java.sql.Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant().atZone(SHANGHAI).toOffsetDateTime();
    }
}
