from __future__ import annotations

from dataclasses import dataclass
import json
import time
from typing import Any, Callable

from cynexus_data.domain.stock.a_stock_persistence import AStockBatch, normalize_dataset
from cynexus_data.pipelines.a_stock.market_information_content import (
    extract_and_persist,
    identities_from_batches,
    recent_backfill_identities,
)
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.baidu.client import BaiduStockClient
from cynexus_data.sources.cailian.client import CailianClient
from cynexus_data.sources.cninfo.client import CninfoClient
from cynexus_data.sources.eastmoney import a_stock as em
from cynexus_data.sources.eastmoney.http import EastMoneyHttpClient
from cynexus_data.sources.iwencai.client import IwencaiClient, dedup_articles
from cynexus_data.sources.market_information.client import OfficialMarketInformationClient
from cynexus_data.sources.sina.a_stock import SinaAStockClient
from cynexus_data.sources.tdx.mootdx_market import fetch_order_book, fetch_transactions
from cynexus_data.sources.tdx.easy_tdx import fetch_history_stock_fund_flow_payload
from cynexus_data.sources.ths.a_stock import ThsAStockClient
from cynexus_data.storage.clickhouse.a_stock import AStockClickHouseRepository
from cynexus_data.storage.postgres.a_stock import AStockPostgresRepository
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository


FetchCallable = Callable[[dict[str, Any]], Any]


@dataclass(frozen=True)
class DatasetSpec:
    provider: str
    target: str
    requires_code: bool
    default_sleep_seconds: float
    fetch: FetchCallable


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    params = dict(request.params or {})
    datasets = _list_param(params.get("datasets") or params.get("dataset"))
    if not datasets:
        raise ValueError("dataset or datasets is required")

    create_table = _bool(params.get("create_table"), False)
    fetch_live = _bool(params.get("fetch_live"), not request.dry_run)
    codes = _resolve_codes(context, params)
    limit_codes = _int(params.get("limit_codes"))
    if limit_codes is not None:
        codes = codes[:limit_codes]
    ignore_errors = _bool(params.get("ignore_errors"), False)

    pg_batches: dict[str, list[dict[str, Any]]] = {}
    ch_batches: dict[str, list[dict[str, Any]]] = {}
    dataset_results: dict[str, Any] = {}

    for dataset in datasets:
        spec = DATASETS.get(dataset)
        if spec is None:
            raise ValueError(f"unsupported A-stock dataset: {dataset}")
        planned_codes = codes if spec.requires_code else [""]
        if spec.requires_code and not planned_codes and "payload" not in params:
            raise ValueError(f"{dataset} requires code/codes/symbol/symbols")

        dataset_rows_read = 0
        dataset_pg_rows = 0
        dataset_ch_rows = 0
        samples: list[Any] = []
        errors: list[str] = []
        payload_sources: set[str] = set()
        fallback_count = 0
        history_complete_count = 0

        for index, code in enumerate(planned_codes, start=1):
            scoped_params = {**params, "dataset": dataset}
            if code:
                scoped_params["code"] = code
            try:
                payload = _provided_payload(params, dataset)
                if payload is None and fetch_live:
                    payload = spec.fetch(scoped_params)
                    _sleep_after_fetch(spec, params)
                elif payload is None:
                    payload = []

                if isinstance(payload, dict):
                    if payload.get("source"):
                        payload_sources.add(str(payload["source"]))
                    for attempt in payload.get("provider_attempts") or []:
                        if attempt.get("provider") and attempt.get("ok"):
                            payload_sources.add(str(attempt["provider"]))
                    if payload.get("mode") == "daily_fallback":
                        fallback_count += 1
                    if payload.get("history_complete") is True:
                        history_complete_count += 1

                batch = normalize_dataset(dataset, payload, scoped_params)
                _merge(pg_batches, batch.postgres)
                _merge(ch_batches, batch.clickhouse)

                dataset_rows_read += batch.rows_read
                dataset_pg_rows += sum(len(rows) for rows in batch.postgres.values())
                dataset_ch_rows += sum(len(rows) for rows in batch.clickhouse.values())
                if len(samples) < 3:
                    samples.append(_sample_payload(payload))
            except Exception as exc:  # noqa: BLE001 - batch jobs should report per-code provider failures.
                message = f"{dataset}:{code or '*'}:{type(exc).__name__}: {exc}"
                errors.append(message)
                if not ignore_errors:
                    raise

            if index >= _int(params.get("batch_limit") or 10_000):
                break

        dataset_results[dataset] = {
            "provider": spec.provider,
            "target": spec.target,
            "planned_codes": len(planned_codes),
            "rows_normalized": dataset_rows_read,
            "postgres_rows": dataset_pg_rows,
            "clickhouse_rows": dataset_ch_rows,
            "samples": samples,
            "errors": errors[:20],
            "error_count": len(errors),
            "sources": sorted(payload_sources),
            "daily_fallback_count": fallback_count,
            "history_complete_count": history_complete_count,
        }

    rows_written = 0
    if request.dry_run:
        notes = ["dry_run skipped database writes"]
        if not fetch_live:
            notes.append("dry_run skipped live provider fetch; pass fetch_live=true to probe source connectivity")
        return {
            "dataset_results": dataset_results,
            "rows_read": sum(int(item.get("rows_normalized") or 0) for item in dataset_results.values()),
            "rows_written": 0,
            "postgres_tables": {table: len(rows) for table, rows in pg_batches.items()},
            "clickhouse_tables": {table: len(rows) for table, rows in ch_batches.items()},
            "notes": notes,
        }

    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    if pg_batches:
        pg_repo = AStockPostgresRepository(context.hooks.postgres.get_client())
        if create_table:
            pg_repo.create_tables()
        rows_written += pg_repo.upsert_batches(pg_batches)
        information_identities = identities_from_batches(pg_batches)
        if information_identities and _bool(params.get("extract_market_information_content"), True):
            backfill_limit = _int(params.get("market_information_content_backfill_limit")) or 0
            information_identities.extend(recent_backfill_identities(pg_repo.client, backfill_limit))
            content_result = extract_and_persist(pg_repo.client, information_identities)
            dataset_results["market_information_content"] = {
                **content_result,
                # Content extraction is a post-processing stage, not another source dataset.
                "rows_normalized": 0,
            }
    if ch_batches:
        ch_repo = AStockClickHouseRepository(context.hooks.clickhouse.get_client())
        if create_table:
            ch_repo.create_tables()
        rows_written += ch_repo.insert_batches(ch_batches)

    return {
        "dataset_results": dataset_results,
        "rows_read": sum(int(item.get("rows_normalized") or 0) for item in dataset_results.values()),
        "rows_written": rows_written,
        "postgres_tables": {table: len(rows) for table, rows in pg_batches.items()},
        "clickhouse_tables": {table: len(rows) for table, rows in ch_batches.items()},
    }


def _em_client() -> EastMoneyHttpClient:
    return _EASTMONEY_CLIENT


_EASTMONEY_CLIENT = EastMoneyHttpClient()


def _fetch_eastmoney_reports(params: dict[str, Any]) -> Any:
    return em.reports(_em_client(), str(params["code"]), max_pages=_int(params.get("max_pages")) or 1)


def _fetch_eastmoney_industry_reports(params: dict[str, Any]) -> Any:
    return em.industry_reports(_em_client(), industry_code=str(params.get("industry_code") or "*"), max_pages=_int(params.get("max_pages")) or 1, begin=str(params.get("begin") or "2024-01-01"))


def _fetch_eastmoney_stock_info(params: dict[str, Any]) -> Any:
    return em.stock_info(_em_client(), str(params["code"]))


def _fetch_eastmoney_concept_blocks(params: dict[str, Any]) -> Any:
    return em.concept_blocks(_em_client(), str(params["code"]))


def _fetch_eastmoney_fund_flow_minute(params: dict[str, Any]) -> Any:
    return em.fund_flow_minute(_em_client(), str(params["code"]))


def _fetch_eastmoney_fund_flow_120d(params: dict[str, Any]) -> Any:
    return em.fund_flow_120d(
        _em_client(),
        str(params["code"]),
        mode=str(params.get("fund_flow_mode") or "auto").strip().lower(),
    )


def _fetch_easy_tdx_fund_flow_120d(params: dict[str, Any]) -> Any:
    return fetch_history_stock_fund_flow_payload(
        str(params["code"]),
        count=_int(params.get("fund_flow_count") or params.get("count")) or 120,
        timeout=float(params.get("provider_timeout") or 15),
    )


def _fetch_stock_fund_flow_120d(params: dict[str, Any]) -> Any:
    primary = str(params.get("fund_flow_provider") or params.get("provider") or "easy_tdx").strip().lower()
    order = [primary] + [item for item in ("easy_tdx", "eastmoney") if item != primary]
    attempts: list[dict[str, Any]] = []
    for provider in order:
        try:
            payload = _fetch_easy_tdx_fund_flow_120d(params) if provider == "easy_tdx" else _fetch_eastmoney_fund_flow_120d(params)
            if isinstance(payload, dict) and payload.get("items"):
                payload = dict(payload)
                payload["provider_attempts"] = attempts + [{"provider": provider, "ok": True, "rows": len(payload.get("items") or [])}]
                return payload
            attempts.append({"provider": provider, "ok": False, "error": "empty_response"})
        except Exception as exc:  # noqa: BLE001 - fallback is part of the source contract.
            attempts.append({"provider": provider, "ok": False, "error": f"{type(exc).__name__}: {exc}"})
    raise RuntimeError("all stock fund-flow providers failed: " + "; ".join(str(item) for item in attempts))


def _fetch_eastmoney_industry_rank(params: dict[str, Any]) -> Any:
    return em.industry_rank(_em_client(), top_n=_int(params.get("top_n")) or 20)


def _fetch_eastmoney_daily_dragon_tiger(params: dict[str, Any]) -> Any:
    return em.daily_dragon_tiger(_em_client(), trade_date=params.get("trade_date"), min_net_buy=_float(params.get("min_net_buy")))


def _fetch_eastmoney_dragon_tiger(params: dict[str, Any]) -> Any:
    return em.dragon_tiger_board(_em_client(), str(params["code"]), str(params.get("trade_date")), look_back=_int(params.get("look_back")) or 30)


def _fetch_eastmoney_lockup(params: dict[str, Any]) -> Any:
    return em.lockup_expiry(_em_client(), str(params["code"]), str(params.get("trade_date")), forward_days=_int(params.get("forward_days")) or 90)


def _fetch_eastmoney_block_trade(params: dict[str, Any]) -> Any:
    return em.block_trade(_em_client(), str(params["code"]), page_size=_int(params.get("page_size")) or 20)


def _fetch_eastmoney_holders(params: dict[str, Any]) -> Any:
    return em.holder_num_change(_em_client(), str(params["code"]), page_size=_int(params.get("page_size")) or 10)


def _fetch_eastmoney_dividends(params: dict[str, Any]) -> Any:
    return em.dividend_history(_em_client(), str(params["code"]), page_size=_int(params.get("page_size")) or 20)


def _fetch_eastmoney_stock_news(params: dict[str, Any]) -> Any:
    return em.stock_news(_em_client(), str(params["code"]), page_size=_int(params.get("page_size")) or 20)


def _fetch_sina_stock_news(params: dict[str, Any]) -> Any:
    return SinaAStockClient().stock_news(str(params["code"]), page=_int(params.get("page_num")) or 1)


def _fetch_eastmoney_global_news(params: dict[str, Any]) -> Any:
    return em.global_news(_em_client(), page_size=_int(params.get("eastmoney_news_page_size")) or _int(params.get("page_size")) or 50)


def _fetch_cailian_telegraph(params: dict[str, Any]) -> Any:
    return CailianClient().telegraph(page_size=_int(params.get("cailian_page_size")) or _int(params.get("page_size")) or 50)


def _fetch_official_market_information(params: dict[str, Any]) -> Any:
    keys = _list_param(params.get("source_keys") or params.get("sources"))
    return OfficialMarketInformationClient().fetch(source_keys=keys or None, page_size=_int(params.get("official_page_size")) or _int(params.get("page_size")) or 40)


def _fetch_eastmoney_limit_pool(params: dict[str, Any]) -> Any:
    return em.limit_pool(_em_client(), str(params.get("pool") or "zt"), str(params.get("trade_date") or ""))


def _fetch_eastmoney_hot_rank(params: dict[str, Any]) -> Any:
    return em.hot_rank(_em_client(), top=_int(params.get("top")) or 50)


def _fetch_eastmoney_hot_concept(params: dict[str, Any]) -> Any:
    return em.hot_concept(_em_client(), str(params["code"]))


def _fetch_ths_eps(params: dict[str, Any]) -> Any:
    return ThsAStockClient().eps_forecast(str(params["code"]))


def _fetch_ths_hot_reason(params: dict[str, Any]) -> Any:
    return ThsAStockClient().hot_reason(params.get("trade_date"))


def _fetch_ths_northbound(params: dict[str, Any]) -> Any:
    return ThsAStockClient().northbound_realtime()


def _fetch_ths_limit_up_pool(params: dict[str, Any]) -> Any:
    return ThsAStockClient().limit_up_pool(str(params.get("trade_date")))


def _fetch_ths_hot_list(params: dict[str, Any]) -> Any:
    return ThsAStockClient().hot_list(str(params.get("period") or "hour"))


def _fetch_cninfo_announcements(params: dict[str, Any]) -> Any:
    return CninfoClient().announcements(str(params["code"]), page_size=_int(params.get("page_size")) or 30)


def _fetch_cninfo_market_announcements(params: dict[str, Any]) -> Any:
    return CninfoClient().market_announcements(page_size=_int(params.get("cninfo_market_page_size")) or _int(params.get("page_size")) or 200)


def _fetch_cninfo_regulatory_announcements(params: dict[str, Any]) -> Any:
    return CninfoClient().regulatory_announcements(page_size=_int(params.get("cninfo_regulatory_page_size")) or _int(params.get("page_size")) or 100)


def _fetch_cninfo_irm(params: dict[str, Any]) -> Any:
    return CninfoClient().irm(str(params["code"]), page_size=_int(params.get("page_size")) or 30, page_num=_int(params.get("page_num")) or 1)


def _fetch_sina_financial(params: dict[str, Any]) -> Any:
    return SinaAStockClient().financial_report(str(params["code"]), report_type=str(params.get("report_type") or "lrb"), num=_int(params.get("num")) or 8)


def _fetch_sina_option_codes(params: dict[str, Any]) -> Any:
    return SinaAStockClient().option_codes(underlying=str(params.get("underlying") or "510050"), call=_bool(params.get("call"), True))


def _fetch_sina_option_tquote(params: dict[str, Any]) -> Any:
    return SinaAStockClient().option_tquote(str(params.get("option_code") or params.get("code")))


def _fetch_sina_option_greeks(params: dict[str, Any]) -> Any:
    return SinaAStockClient().option_greeks(str(params.get("option_code") or params.get("code")))


def _fetch_iwencai_search(params: dict[str, Any]) -> Any:
    rows = IwencaiClient().search(str(params.get("query") or ""), channel=str(params.get("channel") or "report"), size=_int(params.get("size")) or 50)
    return dedup_articles(rows) if _bool(params.get("dedup"), True) else rows


def _fetch_iwencai_query(params: dict[str, Any]) -> Any:
    return IwencaiClient().query2data(str(params.get("query") or ""), page=_int(params.get("page")) or 1, limit=_int(params.get("limit")) or 50)


def _fetch_tdx_transactions(params: dict[str, Any]) -> Any:
    return fetch_transactions(str(params["code"]), trade_date=params.get("trade_date"))


def _fetch_tdx_order_book(params: dict[str, Any]) -> Any:
    return fetch_order_book(str(params["code"]))


def _fetch_baidu_kline(params: dict[str, Any]) -> Any:
    return BaiduStockClient().kline_with_ma(str(params["code"]), start_time=str(params.get("start_time") or ""))


DATASETS: dict[str, DatasetSpec] = {
    "eastmoney_reports": DatasetSpec("eastmoney", "postgres", True, 2.0, _fetch_eastmoney_reports),
    "eastmoney_industry_reports": DatasetSpec("eastmoney", "postgres", False, 2.0, _fetch_eastmoney_industry_reports),
    "eastmoney_stock_info": DatasetSpec("eastmoney", "postgres", True, 2.0, _fetch_eastmoney_stock_info),
    "eastmoney_concept_blocks": DatasetSpec("eastmoney", "postgres", True, 2.0, _fetch_eastmoney_concept_blocks),
    "eastmoney_fund_flow_minute": DatasetSpec("eastmoney", "clickhouse", True, 5.0, _fetch_eastmoney_fund_flow_minute),
    "eastmoney_fund_flow_120d": DatasetSpec("easy_tdx+eastmoney", "postgres", True, 0.2, _fetch_stock_fund_flow_120d),
    "eastmoney_industry_rank": DatasetSpec("eastmoney", "postgres", False, 2.0, _fetch_eastmoney_industry_rank),
    "eastmoney_daily_dragon_tiger": DatasetSpec("eastmoney", "postgres", False, 2.0, _fetch_eastmoney_daily_dragon_tiger),
    "eastmoney_dragon_tiger": DatasetSpec("eastmoney", "postgres", True, 2.0, _fetch_eastmoney_dragon_tiger),
    "eastmoney_lockup": DatasetSpec("eastmoney", "postgres", True, 2.0, _fetch_eastmoney_lockup),
    "eastmoney_block_trade": DatasetSpec("eastmoney", "postgres", True, 2.0, _fetch_eastmoney_block_trade),
    "eastmoney_holders": DatasetSpec("eastmoney", "postgres", True, 2.0, _fetch_eastmoney_holders),
    "eastmoney_dividends": DatasetSpec("eastmoney", "postgres", True, 2.0, _fetch_eastmoney_dividends),
    "eastmoney_stock_news": DatasetSpec("eastmoney", "postgres", True, 2.0, _fetch_eastmoney_stock_news),
    "sina_stock_news": DatasetSpec("sina", "postgres", True, 1.0, _fetch_sina_stock_news),
    "eastmoney_global_news": DatasetSpec("eastmoney", "postgres", False, 2.0, _fetch_eastmoney_global_news),
    "cailian_telegraph": DatasetSpec("cailian", "postgres", False, 1.0, _fetch_cailian_telegraph),
    "official_market_information": DatasetSpec("official", "postgres", False, 2.0, _fetch_official_market_information),
    "eastmoney_limit_pool": DatasetSpec("eastmoney", "postgres", False, 2.0, _fetch_eastmoney_limit_pool),
    "eastmoney_hot_rank": DatasetSpec("eastmoney", "postgres", False, 5.0, _fetch_eastmoney_hot_rank),
    "eastmoney_hot_concept": DatasetSpec("eastmoney", "postgres", True, 5.0, _fetch_eastmoney_hot_concept),
    "ths_eps_forecast": DatasetSpec("ths", "postgres", True, 1.0, _fetch_ths_eps),
    "ths_hot_reason": DatasetSpec("ths", "postgres", False, 1.0, _fetch_ths_hot_reason),
    "ths_northbound": DatasetSpec("ths", "clickhouse", False, 1.0, _fetch_ths_northbound),
    "ths_limit_up_pool": DatasetSpec("ths", "postgres", False, 1.0, _fetch_ths_limit_up_pool),
    "ths_hot_list": DatasetSpec("ths", "postgres", False, 1.0, _fetch_ths_hot_list),
    "cninfo_announcements": DatasetSpec("cninfo", "postgres", True, 1.5, _fetch_cninfo_announcements),
    "cninfo_market_announcements": DatasetSpec("cninfo", "postgres", False, 1.5, _fetch_cninfo_market_announcements),
    "cninfo_regulatory_announcements": DatasetSpec("cninfo", "postgres", False, 1.5, _fetch_cninfo_regulatory_announcements),
    "cninfo_irm": DatasetSpec("cninfo", "postgres", True, 1.5, _fetch_cninfo_irm),
    "sina_financial_report": DatasetSpec("sina", "postgres", True, 0.5, _fetch_sina_financial),
    "sina_option_codes": DatasetSpec("sina", "postgres", False, 0.5, _fetch_sina_option_codes),
    "sina_option_tquote": DatasetSpec("sina", "clickhouse", False, 0.5, _fetch_sina_option_tquote),
    "sina_option_greeks": DatasetSpec("sina", "clickhouse", False, 0.5, _fetch_sina_option_greeks),
    "iwencai_search": DatasetSpec("iwencai", "postgres", False, 3.0, _fetch_iwencai_search),
    "iwencai_query": DatasetSpec("iwencai", "postgres", False, 3.0, _fetch_iwencai_query),
    "tdx_transactions": DatasetSpec("mootdx", "clickhouse", True, 0.2, _fetch_tdx_transactions),
    "tdx_order_book": DatasetSpec("mootdx", "clickhouse", True, 0.2, _fetch_tdx_order_book),
    "baidu_kline_ma": DatasetSpec("baidu", "clickhouse", True, 1.0, _fetch_baidu_kline),
}


def _merge(target: dict[str, list[dict[str, Any]]], source: dict[str, list[dict[str, Any]]]) -> None:
    for table, rows in source.items():
        target.setdefault(table, []).extend(rows)


def _provided_payload(params: dict[str, Any], dataset: str) -> Any:
    payloads = params.get("payloads")
    if isinstance(payloads, str):
        payloads = _json_param(payloads)
    if isinstance(payloads, dict) and dataset in payloads:
        return payloads[dataset]
    payload = params.get("payload")
    if isinstance(payload, str):
        return _json_param(payload)
    return payload


def _resolve_codes(context: PipelineContext, params: dict[str, Any]) -> list[str]:
    value = params.get("codes") or params.get("symbols") or params.get("code") or params.get("symbol")
    explicit = _list_param(value)
    if explicit:
        return explicit
    source = str(params.get("code_source") or "").strip().lower()
    if not source:
        return []
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required to resolve code_source")
    if source in {"sec_stock_basic", "stock_basic", "all", "active"}:
        return StockBasicRepository(context.hooks.postgres.get_client()).list_codes(active_only=True)
    if source in {"self_selected", "watchlist"}:
        return _self_selected_codes(context)
    if source in {"fund_flow_history_missing", "missing_fund_flow_history"}:
        return _missing_fund_flow_history_codes(context)
    raise ValueError(f"unsupported code_source: {source}")


def _self_selected_codes(context: PipelineContext) -> list[str]:
    if context.hooks is None:
        return []
    from sqlalchemy import text

    with context.hooks.postgres.get_client().session() as db:
        rows = db.execute(
            text(
                """
                SELECT DISTINCT symbol AS code
                  FROM cynexus_appl_market.self_selected_list_items
                 WHERE deleted_at IS NULL
                   AND status = 'active'
                   AND target_type = 'stock'
                   AND symbol IS NOT NULL
                   AND symbol <> ''
                 ORDER BY symbol
                """
            )
        ).all()
    return [str(row[0]).strip() for row in rows if str(row[0]).strip()]


def _missing_fund_flow_history_codes(context: PipelineContext) -> list[str]:
    if context.hooks is None:
        return []
    from sqlalchemy import text

    with context.hooks.postgres.get_client().session() as db:
        rows = db.execute(
            text(
                """
                WITH selected AS (
                    SELECT DISTINCT symbol AS code
                      FROM cynexus_appl_market.self_selected_list_items
                     WHERE deleted_at IS NULL
                       AND status = 'active'
                       AND target_type = 'stock'
                       AND symbol IS NOT NULL
                       AND symbol <> ''
                ), coverage AS (
                    SELECT code, count(*) AS history_rows
                      FROM cynexus_appl_market.a_stock_fund_flow_daily
                     WHERE source IN ('easy_tdx.tdx', 'eastmoney.push2his', 'eastmoney.push2delay')
                     GROUP BY code
                )
                SELECT selected.code
                  FROM selected
                  LEFT JOIN coverage ON coverage.code = selected.code
                 WHERE COALESCE(coverage.history_rows, 0) < 120
                 ORDER BY COALESCE(coverage.history_rows, 0), selected.code
                """
            )
        ).all()
    return [str(row[0]).strip() for row in rows if str(row[0]).strip()]


def _list_param(value: Any) -> list[str]:
    if value is None:
        return []
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]
    text = str(value).strip()
    if not text:
        return []
    return [item.strip() for item in text.split(",") if item.strip()]


def _sleep_after_fetch(spec: DatasetSpec, params: dict[str, Any]) -> None:
    seconds = _float(params.get("request_sleep_seconds"))
    if seconds is None:
        seconds = spec.default_sleep_seconds
    if seconds > 0:
        time.sleep(seconds)


def _sample_payload(payload: Any) -> Any:
    if isinstance(payload, list):
        return payload[:2]
    if isinstance(payload, dict) and isinstance(payload.get("items"), list):
        return {"items": payload["items"][:2], "count": len(payload["items"])}
    return payload


def _bool(value: Any, default: bool = False) -> bool:
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}


def _int(value: Any) -> int | None:
    if value in (None, ""):
        return None
    try:
        return int(float(str(value)))
    except (TypeError, ValueError):
        return None


def _float(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(str(value))
    except (TypeError, ValueError):
        return None


def _json_param(value: str) -> Any:
    text = value.strip()
    if not text:
        return None
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return value
