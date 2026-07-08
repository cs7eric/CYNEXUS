from __future__ import annotations

from cynexus_data.quality import data_source_health as health


def test_health_summary_counts_success_rate_and_latency():
    items = [
        health.DataSourceProbeResult(
            source_code="ok",
            source_name="OK",
            source_type="http",
            category="market-data",
            provider="test",
            status="healthy",
            latency_ms=10,
        ),
        health.DataSourceProbeResult(
            source_code="bad",
            source_name="Bad",
            source_type="http",
            category="market-data",
            provider="test",
            status="failed",
            latency_ms=30,
        ),
        health.DataSourceProbeResult(
            source_code="skip",
            source_name="Skip",
            source_type="tcp",
            category="market-data",
            provider="test",
            status="skipped",
        ),
    ]

    summary = health._summary(items)

    assert summary["total"] == 3
    assert summary["healthy"] == 1
    assert summary["failed"] == 1
    assert summary["skipped"] == 1
    assert summary["success_rate"] == 33.33
    assert summary["avg_latency_ms"] == 20


def test_iwencai_probe_is_skipped_without_api_key(monkeypatch):
    monkeypatch.delenv("IWENCAI_API_KEY", raising=False)

    result = health._probe_iwencai()

    assert result.status == "skipped"
    assert result.connectivity is False
    assert result.checks["api_key_configured"] is False


def test_python_import_probe_reports_missing_module():
    result = health._probe_python_import("missing", "Missing", "test", "cynexus_data_missing_module")

    assert result.status == "failed"
    assert result.connectivity is False
    assert result.checks["exception"] == "ModuleNotFoundError"
