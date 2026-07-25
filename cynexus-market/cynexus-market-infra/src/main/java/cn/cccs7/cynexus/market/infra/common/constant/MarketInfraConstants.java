package cn.cccs7.cynexus.market.infra.common.constant;

import java.util.List;

public final class MarketInfraConstants {

    public static final List<String> DATE_COLUMN_PRIORITY = List.of(
            "trade_date",
            "trading_date",
            "snapshot_date",
            "capture_date",
            "data_date",
            "stat_date",
            "report_date",
            "target_date",
            "biz_date",
            "last_trade_date",
            "last_seen",
            "ex_date",
            "date",
            "dt",
            "trade_time",
            "quote_time",
            "updated_at_result",
            "captured_at",
            "ingested_at",
            "fetched_at",
            "source_updated_at",
            "updated_at",
            "created_at"
    );

    private MarketInfraConstants() {
    }
}
