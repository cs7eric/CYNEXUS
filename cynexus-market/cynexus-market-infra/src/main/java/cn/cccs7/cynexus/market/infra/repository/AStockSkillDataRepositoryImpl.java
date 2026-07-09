package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.AStockSkillDataBO;
import cn.cccs7.cynexus.market.domain.repository.AStockSkillDataRepository;
import cn.cccs7.cynexus.market.infra.client.ClickHouseAStockSkillDataClient;
import cn.cccs7.cynexus.market.infra.client.PostgresAStockSkillDataClient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class AStockSkillDataRepositoryImpl implements AStockSkillDataRepository {

    private static final String PG_SCHEMA = "cynexus_appl_market";
    private static final String CH_DATABASE = "cynexus";

    private static final List<DatasetSpec> DATASETS = List.of(
            pg("research_report", "研报列表", "资讯研报", "东财/同花顺/iwencai 研报、评级、PDF 地址和预测指标。", "a_stock_research_report", "stock/industry/report", "code", "publish_date", "\"publish_date\" desc nulls last, \"ingested_at\" desc",
                    List.of("source", "scope", "code", "industry_code", "industry_name", "report_id", "title", "org_name", "author", "publish_date", "rating", "rating_change", "pdf_url", "url", "metrics", "ingested_at")),
            pg("financial_statement", "财报字段", "基本面", "个股资产负债/利润/现金流等 F10 财报字段。", "a_stock_financial_statement", "stock/report", "code", "report_date", "\"report_date\" desc, \"ingested_at\" desc",
                    List.of("source", "code", "report_type", "report_date", "fields", "ingested_at")),
            pg("announcement", "公告摘要", "资讯研报", "个股公告摘要、公告分类和原文 URL。", "a_stock_announcement", "stock/event", "code", "publish_date", "\"publish_date\" desc nulls last, \"ingested_at\" desc",
                    List.of("source", "code", "announcement_id", "title", "category", "publish_date", "url", "ingested_at")),
            pg("news", "个股新闻", "资讯研报", "东财/全球资讯/股票新闻标题、摘要、媒体和发布时间。", "a_stock_news", "stock/news", "code", "published_at", "\"published_at\" desc nulls last, \"ingested_at\" desc",
                    List.of("source", "code", "news_id", "title", "summary", "media_name", "published_at", "url", "ingested_at")),
            pg("investor_interaction", "互动易问答", "资讯研报", "互动易投资者问答、回答人和问答时间。", "a_stock_investor_interaction", "stock/qa", "code", "asked_at", "\"asked_at\" desc nulls last, \"ingested_at\" desc",
                    List.of("source", "code", "interaction_id", "company_name", "question", "answer", "answerer", "asked_at", "ingested_at")),
            pg("fund_flow_daily", "个股日级资金流", "资金流", "东财 120 日/日级个股主力、超大、大、中、小单净流。", "a_stock_fund_flow_daily", "stock/day", "code", "trade_date", "\"trade_date\" desc, \"ingested_at\" desc",
                    List.of("source", "code", "trade_date", "main_net", "super_net", "large_net", "mid_net", "small_net", "ingested_at")),
            pg("industry_rank", "行业板块排名", "板块", "东财口径行业涨跌排名、涨跌家数和领涨股。", "a_stock_industry_rank_snapshot", "industry/snapshot", "industry_code", "snapshot_date", "\"snapshot_date\" desc, \"rank_direction\", \"rank_no\" asc",
                    List.of("source", "snapshot_date", "rank_direction", "rank_no", "industry_code", "industry_name", "change_pct", "up_count", "down_count", "leader_name", "leader_change_pct", "ingested_at")),
            pg("dragon_tiger_daily", "龙虎榜全市场", "龙虎榜", "全市场龙虎榜上榜股票、原因、买卖额和净买入。", "a_stock_dragon_tiger_daily", "stock/day", "code", "trade_date", "\"trade_date\" desc, \"code\", \"reason\"",
                    List.of("source", "trade_date", "code", "name", "reason", "close_price", "change_pct", "net_buy_wan", "buy_wan", "sell_wan", "turnover_pct", "ingested_at")),
            pg("dragon_tiger_seat", "龙虎榜席位", "龙虎榜", "龙虎榜营业部席位买入、卖出、净额明细。", "a_stock_dragon_tiger_seat", "stock/day/seat", "code", "trade_date", "\"trade_date\" desc, \"code\", \"side\", \"seat_rank\" asc",
                    List.of("source", "trade_date", "code", "side", "seat_rank", "seat_name", "buy_wan", "sell_wan", "net_wan", "ingested_at")),
            pg("lockup_expiry", "限售解禁", "事件日历", "限售解禁日期、解禁类型、股数和占比。", "a_stock_lockup_expiry", "stock/event", "code", "free_date", "\"free_date\" desc, \"code\"",
                    List.of("source", "code", "free_date", "lockup_type", "shares", "ratio", "ingested_at")),
            pg("block_trade", "大宗交易", "交易事件", "大宗交易成交价、折溢价、成交量、买卖营业部。", "a_stock_block_trade", "stock/day", "code", "trade_date", "\"trade_date\" desc, \"code\"",
                    List.of("source", "code", "trade_date", "price", "close_price", "premium_pct", "volume", "amount", "buyer", "seller", "ingested_at")),
            pg("holder_num", "股东户数", "基本面", "股东户数变化、户均持股和变化比例。", "a_stock_holder_num", "stock/report", "code", "end_date", "\"end_date\" desc, \"code\"",
                    List.of("source", "code", "end_date", "holder_num", "change_num", "change_ratio", "avg_shares", "ingested_at")),
            pg("dividend", "分红送转", "基本面", "分红、送转、除权除息日期和方案。", "a_stock_dividend", "stock/event", "code", "ex_dividend_date", "\"ex_dividend_date\" desc, \"code\"",
                    List.of("source", "code", "ex_dividend_date", "bonus_rmb", "transfer_ratio", "bonus_ratio", "plan", "ingested_at")),
            pg("hot_rank", "人气榜", "热度", "同花顺/东财热榜、人气排名、热度、价格和概念标签。", "a_stock_hot_rank_snapshot", "stock/snapshot", "code", "snapshot_date", "\"snapshot_date\" desc, \"list_type\", \"rank_no\" asc",
                    List.of("source", "snapshot_date", "list_type", "rank_no", "code", "name", "heat", "price", "pct", "rank_change", "concepts", "ingested_at")),
            pg("hot_concept", "热门概念命中", "热度", "热门概念与个股命中关系、命中次数。", "a_stock_hot_concept_snapshot", "stock/snapshot", "code", "snapshot_date", "\"snapshot_date\" desc, \"concept_name\", \"code\"",
                    List.of("source", "snapshot_date", "code", "concept_code", "concept_name", "hit_count", "ingested_at")),
            pg("hot_reason", "THS 题材归因", "热度", "同花顺热点强势股、题材归因和涨停原因。", "a_stock_hot_reason_snapshot", "stock/snapshot", "code", "snapshot_date", "\"snapshot_date\" desc, \"code\", \"reason_key\"",
                    List.of("source", "snapshot_date", "code", "name", "reason_key", "reason", "subject", "ingested_at")),
            pg("limit_pool", "涨跌停池", "涨跌停", "东财/同花顺涨停池、炸板池、跌停池和昨日涨停池。", "a_stock_limit_pool_snapshot", "stock/snapshot", "code", "snapshot_date", "\"snapshot_date\" desc, \"pool\", \"code\"",
                    List.of("source", "snapshot_date", "pool", "code", "name", "price", "pct", "reason", "board_type", "limit_days", "first_time", "last_time", "break_times", "seal_amount", "ingested_at")),
            pg("option_contract", "Sina 期权合约", "期权", "ETF 期权合约月份、认购认沽、合约代码和标的。", "a_stock_option_contract_snapshot", "option/snapshot", "option_code", "snapshot_date", "\"snapshot_date\" desc, \"underlying\", \"option_month\", \"option_type\"",
                    List.of("source", "snapshot_date", "underlying", "option_month", "option_type", "option_code", "ingested_at")),
            pg("concept_membership", "个股概念归属", "板块", "东财概念归属、概念名称、涨跌幅和领涨股。", "a_stock_concept_membership", "stock/snapshot", "code", "snapshot_date", "\"snapshot_date\" desc, \"concept_name\", \"code\"",
                    List.of("source", "snapshot_date", "code", "concept_code", "concept_name", "change_pct", "lead_stock", "ingested_at")),
            ch("fund_flow_minute", "分钟级个股资金流", "资金流", "东财个股分钟级主力、超大、大、中、小单净流。", "ch_ods_stock_fund_flow_minute", "stock/minute", "code", "trade_time", "`trade_time` DESC",
                    List.of("source", "code", "trade_date", "trade_time", "main_net", "super_net", "large_net", "mid_net", "small_net", "ingested_at")),
            ch("transaction", "逐笔成交", "盘口成交", "mootdx/Tencent 个股逐笔成交价格、成交量、金额和买卖方向。", "ch_ods_stock_transaction", "stock/tick", "code", "trade_date", "`trade_date` DESC, `trade_time` DESC",
                    List.of("source", "code", "trade_date", "trade_time", "price", "volume", "amount", "buy_sell", "ingested_at")),
            ch("order_book", "五档盘口", "盘口成交", "盘口快照、五档买卖价量、行情价格和成交额。", "ch_ods_stock_order_book_snapshot", "stock/snapshot", "code", "quote_time", "`quote_time` DESC",
                    List.of("source", "code", "quote_time", "trade_date", "price", "open", "high", "low", "pre_close", "volume", "amount", "bid_prices", "ask_prices", "bid_volumes", "ask_volumes", "ingested_at")),
            ch("baidu_kline_ma", "百度 K 线 MA", "K线", "百度股市通日 K 与 MA5/MA10/MA20。", "ch_ods_baidu_kline_ma", "stock/day", "code", "trade_date", "`trade_date` DESC",
                    List.of("source", "code", "trade_date", "open", "high", "low", "close", "volume", "amount", "ma5", "ma10", "ma20", "ingested_at")),
            ch("northbound_minute", "北向资金分钟流向", "资金流", "同花顺北向资金沪股通/深股通分钟流向。", "ch_ods_northbound_minute", "market/minute", null, "trade_date", "`trade_date` DESC, `minute_time` DESC",
                    List.of("source", "trade_date", "minute_time", "hgt_yi", "sgt_yi", "ingested_at")),
            ch("option_quote", "Sina 期权报价", "期权", "ETF 期权 T 型报价、希腊字母和隐含波动率快照。", "ch_ods_option_quote_snapshot", "option/snapshot", "option_code", "quote_time", "`quote_time` DESC",
                    List.of("source", "option_code", "quote_time", "trade_date", "name", "last", "bid", "ask", "strike", "volume", "amount", "open_interest", "delta", "gamma", "theta", "vega", "iv", "ingested_at"))
    );

    private final PostgresAStockSkillDataClient postgresClient;
    private final ClickHouseAStockSkillDataClient clickHouseClient;

    public AStockSkillDataRepositoryImpl(PostgresAStockSkillDataClient postgresClient,
                                         ClickHouseAStockSkillDataClient clickHouseClient) {
        this.postgresClient = postgresClient;
        this.clickHouseClient = clickHouseClient;
    }

    @Override
    public AStockSkillDataBO.DatasetListBO listDatasets() {
        return AStockSkillDataBO.DatasetListBO.builder()
                .generatedAt(OffsetDateTime.now())
                .datasets(DATASETS.stream().map(this::toDataset).toList())
                .build();
    }

    @Override
    public AStockSkillDataBO.RowsBO listRows(String datasetCode, Integer limit, String code, String source) {
        DatasetSpec spec = find(datasetCode);
        int resolvedLimit = limit == null ? 50 : Math.max(1, Math.min(limit, 500));
        List<Map<String, Object>> rows = spec.store().equals("postgres")
                ? postgresClient.rows(PG_SCHEMA, spec.tableName(), spec.columns(), "source", spec.codeColumn(), spec.sortClause(), resolvedLimit, code, source)
                : clickHouseClient.rows(CH_DATABASE, spec.tableName(), spec.columns(), "source", spec.codeColumn(), spec.sortClause(), resolvedLimit, code, source);
        return AStockSkillDataBO.RowsBO.builder()
                .dataset(toDataset(spec))
                .queriedAt(OffsetDateTime.now())
                .limit(resolvedLimit)
                .code(blankToNull(code))
                .source(blankToNull(source))
                .columns(spec.columns())
                .rows(rows)
                .build();
    }

    private AStockSkillDataBO.DatasetBO toDataset(DatasetSpec spec) {
        try {
            Stats stats = stats(spec);
            return baseDataset(spec)
                    .rowCount(stats.rowCount())
                    .latestAt(stats.latestAt())
                    .build();
        } catch (Exception ex) {
            return baseDataset(spec).error(ex.getMessage()).build();
        }
    }

    private Stats stats(DatasetSpec spec) {
        if (spec.store().equals("postgres")) {
            PostgresAStockSkillDataClient.TableStats stats = postgresClient.stats(PG_SCHEMA, spec.tableName(), spec.dateColumn());
            return new Stats(stats.rowCount(), stats.latestAt());
        }
        ClickHouseAStockSkillDataClient.TableStats stats = clickHouseClient.stats(CH_DATABASE, spec.tableName(), spec.dateColumn());
        return new Stats(stats.rowCount(), stats.latestAt());
    }

    private AStockSkillDataBO.DatasetBO.DatasetBOBuilder baseDataset(DatasetSpec spec) {
        return AStockSkillDataBO.DatasetBO.builder()
                .code(spec.code())
                .name(spec.name())
                .groupName(spec.groupName())
                .description(spec.description())
                .store(spec.store())
                .databaseName(spec.store().equals("postgres") ? "cynexus" : CH_DATABASE)
                .schemaName(spec.store().equals("postgres") ? PG_SCHEMA : CH_DATABASE)
                .tableName(spec.tableName())
                .source("source")
                .grain(spec.grain())
                .codeColumn(spec.codeColumn())
                .dateColumn(spec.dateColumn())
                .defaultSort(spec.sortClause())
                .columns(spec.columns());
    }

    private DatasetSpec find(String datasetCode) {
        String value = datasetCode == null ? "" : datasetCode.trim().toLowerCase(Locale.ROOT);
        return DATASETS.stream()
                .filter(item -> item.code().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown A股 Skill dataset: " + datasetCode));
    }

    private static DatasetSpec pg(String code,
                                  String name,
                                  String groupName,
                                  String description,
                                  String tableName,
                                  String grain,
                                  String codeColumn,
                                  String dateColumn,
                                  String sortClause,
                                  List<String> columns) {
        return new DatasetSpec(code, name, groupName, description, "postgres", tableName, grain, codeColumn, dateColumn, sortClause, columns);
    }

    private static DatasetSpec ch(String code,
                                  String name,
                                  String groupName,
                                  String description,
                                  String tableName,
                                  String grain,
                                  String codeColumn,
                                  String dateColumn,
                                  String sortClause,
                                  List<String> columns) {
        return new DatasetSpec(code, name, groupName, description, "clickhouse", tableName, grain, codeColumn, dateColumn, sortClause, columns);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DatasetSpec(String code,
                               String name,
                               String groupName,
                               String description,
                               String store,
                               String tableName,
                               String grain,
                               String codeColumn,
                               String dateColumn,
                               String sortClause,
                               List<String> columns) {
    }

    private record Stats(Long rowCount, String latestAt) {
    }
}
