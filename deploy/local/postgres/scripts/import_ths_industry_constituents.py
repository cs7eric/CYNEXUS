"""Import local THS industry constituent snapshots into PostgreSQL.

This is a one-off/local migration utility for the Java API migration of the
Stock Overview -> Industry Application page. It does not serve UI traffic and
does not call the old Python API. Source files are the persisted reference JSON
snapshots from the old project.
"""

from __future__ import annotations

import argparse
import json
import os
from datetime import datetime, timedelta, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any

import psycopg
from psycopg.types.json import Jsonb


DEFAULT_REFERENCE_DIR = Path(r"F:\dev-repo\mp4-to-word-new\reference\ths-industry")
DEFAULT_SOURCE = "q.10jqka.com.cn"
DEFAULT_SOURCE_TYPE = "json_import"
CHINA_TZ = timezone(timedelta(hours=8))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference-dir", type=Path, default=DEFAULT_REFERENCE_DIR)
    parser.add_argument("--host", default=os.getenv("POSTGRES_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("POSTGRES_PORT", "25433")))
    parser.add_argument("--database", default=os.getenv("POSTGRES_DB", "cynexus"))
    parser.add_argument("--user", default=os.getenv("POSTGRES_USER", "postgres"))
    parser.add_argument("--password", default=os.getenv("POSTGRES_PASSWORD", "C020611."))
    parser.add_argument("--source", default=DEFAULT_SOURCE)
    parser.add_argument("--source-type", default=DEFAULT_SOURCE_TYPE)
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def parse_datetime(value: str | None) -> datetime | None:
    if not value:
        return None
    text = value.replace("Z", "+00:00")
    parsed = datetime.fromisoformat(text)
    if parsed.tzinfo is None:
        return parsed.replace(tzinfo=CHINA_TZ)
    return parsed


def decimal_or_none(value: Any) -> Decimal | None:
    if value is None:
        return None
    if isinstance(value, int | float):
        return Decimal(str(value))
    text = str(value).strip().replace("%", "").replace(",", "")
    if not text or text in {"--", "亏损", "None", "null"}:
        return None
    try:
        return Decimal(text)
    except InvalidOperation:
        return None


def load_source(reference_dir: Path) -> tuple[dict[str, str], dict[str, Any], list[dict[str, Any]], list[str], dict[str, list[int]], datetime | None]:
    industry_list = json.loads((reference_dir / "industry_list.json").read_text(encoding="utf-8"))
    name_to_code = industry_list.get("nameToCode", {})
    by_code = industry_list.get("byCode", {})

    rows: list[dict[str, Any]] = []
    requested_codes: list[str] = []
    page_counts: dict[str, list[int]] = {}
    latest_fetch: datetime | None = None

    for path in sorted((reference_dir / "constituents").glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        if not data.get("ok"):
            continue
        industry_code = str(data.get("code") or path.stem)
        requested_codes.append(industry_code)
        fetched_at = parse_datetime(data.get("fetchedAt"))
        if fetched_at and (latest_fetch is None or fetched_at > latest_fetch):
            latest_fetch = fetched_at
        page_counts[industry_code] = data.get("pageRowCounts") or []
        for raw in data.get("rows") or []:
            stock_code = str(raw.get("代码") or "").strip().zfill(6)
            if not stock_code:
                continue
            rows.append(
                {
                    "industry_code": industry_code,
                    "stock_code": stock_code,
                    "stock_name": raw.get("名称"),
                    "rank": int(raw["序号"]) if raw.get("序号") not in (None, "") else None,
                    "price": decimal_or_none(raw.get("现价")),
                    "change_pct": decimal_or_none(raw.get("涨跌幅(%)")),
                    "change": decimal_or_none(raw.get("涨跌")),
                    "speed_pct": decimal_or_none(raw.get("涨速(%)")),
                    "turnover_pct": decimal_or_none(raw.get("换手(%)")),
                    "volume_ratio": decimal_or_none(raw.get("量比")),
                    "amplitude_pct": decimal_or_none(raw.get("振幅(%)")),
                    "amount_text": None if raw.get("成交额") in (None, "") else str(raw.get("成交额")),
                    "float_shares_text": None if raw.get("流通股") in (None, "") else str(raw.get("流通股")),
                    "float_market_cap_text": None if raw.get("流通市值") in (None, "") else str(raw.get("流通市值")),
                    "pe": decimal_or_none(raw.get("市盈率")),
                    "captured_at": fetched_at,
                    "extra": raw,
                }
            )
    return name_to_code, by_code, rows, requested_codes, page_counts, latest_fetch


def import_rows(args: argparse.Namespace) -> dict[str, Any]:
    name_to_code, by_code, rows, requested_codes, page_counts, latest_fetch = load_source(args.reference_dir)
    if not rows:
        raise RuntimeError(f"No constituent rows found under {args.reference_dir}")
    snapshot_date = (latest_fetch or datetime.now(CHINA_TZ)).date()
    if args.dry_run:
        return {
            "dryRun": True,
            "snapshotDate": str(snapshot_date),
            "industryCount": len(requested_codes),
            "rowCount": len(rows),
        }

    with psycopg.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        dbname=args.database,
    ) as conn:
        with conn.cursor() as cur:
            cur.execute("select coalesce(max(id), 0) from cynexus_appl_market.ths_industry_constituent_capture_batch")
            batch_id = int(cur.fetchone()[0]) + 1
            cur.execute("select coalesce(max(id), 0) from cynexus_appl_market.ths_industry_constituent_snapshot")
            next_snapshot_id = int(cur.fetchone()[0]) + 1

            cur.execute(
                """
                update cynexus_appl_market.ths_industry_constituent_snapshot
                   set deleted_at = now(), updated_at = now()
                 where deleted_at is null
                   and snapshot_date = %s
                   and source = %s
                """,
                (snapshot_date, args.source),
            )
            cur.execute(
                """
                update cynexus_appl_market.ths_industry_constituent_capture_batch
                   set deleted_at = now(), updated_at = now()
                 where deleted_at is null
                   and snapshot_date = %s
                   and source = %s
                """,
                (snapshot_date, args.source),
            )
            cur.execute(
                """
                insert into cynexus_appl_market.ths_industry_constituent_capture_batch
                    (id, snapshot_date, source_type, status, source, requested_industry_codes,
                     industry_count, row_count, page_row_counts, fetched_at, extra)
                values (%s, %s, %s, 'success', %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    batch_id,
                    snapshot_date,
                    args.source_type,
                    args.source,
                    Jsonb(requested_codes),
                    len(requested_codes),
                    len(rows),
                    Jsonb(page_counts),
                    latest_fetch,
                    Jsonb({"sourceDir": str(args.reference_dir), "industryNameByCode": by_code}),
                ),
            )
            cur.executemany(
                """
                insert into cynexus_appl_market.ths_industry_constituent_snapshot
                    (id, batch_id, snapshot_date, industry_code, stock_code, stock_name, rank,
                     price, change_pct, change, speed_pct, turnover_pct, volume_ratio, amplitude_pct,
                     amount_text, float_shares_text, float_market_cap_text, pe,
                     source, source_type, captured_at, extra)
                values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                [
                    (
                        next_snapshot_id + idx,
                        batch_id,
                        snapshot_date,
                        row["industry_code"],
                        row["stock_code"],
                        row["stock_name"],
                        row["rank"],
                        row["price"],
                        row["change_pct"],
                        row["change"],
                        row["speed_pct"],
                        row["turnover_pct"],
                        row["volume_ratio"],
                        row["amplitude_pct"],
                        row["amount_text"],
                        row["float_shares_text"],
                        row["float_market_cap_text"],
                        row["pe"],
                        args.source,
                        args.source_type,
                        row["captured_at"],
                        Jsonb(row["extra"]),
                    )
                    for idx, row in enumerate(rows)
                ],
            )

            cur.executemany(
                """
                update cynexus_appl_market.sector_fund_flow_daily_snapshots
                   set sector_code = %s,
                       updated_at = now()
                 where deleted_at is null
                   and scope = 'industry'
                   and sector_name = %s
                   and (sector_code is null or sector_code = '')
                """,
                [(code, name) for name, code in name_to_code.items()],
            )

    return {
        "dryRun": False,
        "snapshotDate": str(snapshot_date),
        "batchId": batch_id,
        "industryCount": len(requested_codes),
        "rowCount": len(rows),
        "latestFetchedAt": latest_fetch.isoformat() if latest_fetch else None,
    }


def main() -> None:
    print(json.dumps(import_rows(parse_args()), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
