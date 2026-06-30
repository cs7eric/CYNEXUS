from __future__ import annotations

import json
from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi import build_msi, rebuild_msi
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres import msi as msi_storage
from cynexus_data.storage.postgres.msi import MsiRepository


def test_build_msi_dry_run_with_components() -> None:
    components = {"vol": 80, "turnover": 60}

    result = build_msi.run(
        PipelineContext(),
        JobRequest(job_key="build_msi", dry_run=True, params={"trade_date": "2026-06-26", "components": json.dumps(components)}),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 0
    assert result["payload"]["compositeScore"] == 56.0


def test_build_msi_dry_run_require_full_skips() -> None:
    result = build_msi.run(PipelineContext(), JobRequest(job_key="build_msi", dry_run=True, params={"trade_date": "2026-06-26", "vol": 80, "require_full": True}))

    assert result["skipped"] is True
    assert result["skip_reason"] == "no_components_or_require_full_not_met"


def test_rebuild_msi_dry_run_counts_dates() -> None:
    result = rebuild_msi.run(PipelineContext(), JobRequest(job_key="rebuild_msi", dry_run=True, params={"start_date": "2026-06-24", "end_date": "2026-06-26"}))

    assert result["planned_dates"] == 3
    assert result["rows_written"] == 0


def test_component_scores_use_history_percentiles_for_raw_components(monkeypatch) -> None:
    class FakeClient:
        schema = "cynexus_appl_market"

    class FakePercentileRepo:
        def __init__(self, client) -> None:
            self.client = client

        def percentile_score(self, table, column, target_date, current_value):
            return {
                ("ma_count_daily", "new_high_252d_pct"): 12.0,
                ("ma_count_daily", "breadth_raw"): 88.0,
                ("profit_effect_daily", "score"): 34.0,
            }.get((table, column), 50.0)

    values = {
        ("msi_volatility_daily", "sentiment_score"): 70.0,
        ("msi_turnover_activity_daily", "score"): 55.0,
        ("msi_ma_count_daily", "price_strength_score"): 1.0,
        ("msi_ma_count_daily", "new_high_252d_pct"): 1.0,
        ("msi_risk_appetite_daily", "score"): 61.0,
        ("msi_ma_count_daily", "breadth_score"): 40.0,
        ("msi_ma_count_daily", "breadth_raw"): 40.0,
        ("msi_limit_emotion_daily", "composite_score"): 66.0,
        ("msi_profit_effect_daily", "score"): 72.0,
        ("msi_sector_breadth_daily", "advance_pct"): 0.7,
        ("msi_style_risk_daily", "score"): 44.0,
    }
    repo = MsiRepository(FakeClient())
    monkeypatch.setattr(msi_storage, "PercentileRepository", FakePercentileRepo)
    monkeypatch.setattr(repo, "_fetch_optional_column", lambda table, column, trade_date: values.get((table, column)))

    scores = repo.read_component_scores(date(2026, 6, 26))

    assert scores["price_strength"] == 12.0
    assert scores["breadth"] == 88.0
    assert scores["profit_effect"] == 34.0
    assert scores["turnover"] == 55.0
    assert scores["sector_breadth"] == 70.0
