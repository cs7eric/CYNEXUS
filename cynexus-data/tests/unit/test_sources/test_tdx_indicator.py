from datetime import datetime, timezone

from cynexus_data.sources.tdx.indicator import fetch_indicator_snapshots


class FakeClient:
    def get_a_share_codes_all(self):
        return ["688277", "000001"]

    def get_quote(self, codes):
        return [
            {"code": "688277", "last_price": 0, "pre_close_price": 19.09},
            {"code": "000001", "last_price": 11, "pre_close_price": 10},
        ]


def test_fetch_indicator_snapshots_skips_zero_price_placeholders():
    rows = fetch_indicator_snapshots(
        FakeClient(),
        {
            "688277": {"name": "天智航", "market": "SH", "limit_up_threshold": 10},
            "000001": {"name": "平安银行", "market": "SZ", "limit_up_threshold": 10},
        },
        captured_at=datetime(2026, 7, 28, 1, 42, tzinfo=timezone.utc),
    )

    assert [row["code"] for row in rows] == ["000001"]


class RetryClient:
    def __init__(self):
        self.calls = 0

    def get_a_share_codes_all(self):
        return ["000001"]

    def get_quote(self, codes):
        self.calls += 1
        if self.calls == 1:
            return []
        return [{"code": "000001", "last_price": 11, "pre_close_price": 10}]


def test_fetch_indicator_snapshots_retries_empty_provider_page_without_publishing_partial_rows():
    client = RetryClient()

    rows = fetch_indicator_snapshots(
        client,
        {"000001": {"name": "平安银行", "market": "SZ", "limit_up_threshold": 10}},
        captured_at=datetime(2026, 7, 28, 1, 42, tzinfo=timezone.utc),
        retry_attempts=1,
        retry_backoff_seconds=0,
        request_spacing_seconds=0,
    )

    assert client.calls == 2
    assert [row["code"] for row in rows] == ["000001"]


class SplitClient:
    def __init__(self):
        self.calls = []

    def get_a_share_codes_all(self):
        return ["000001", "000002"]

    def get_quote(self, codes):
        self.calls.append(list(codes))
        if len(codes) > 1:
            raise RuntimeError("payload too large")
        code = codes[0][2:] if str(codes[0]).startswith(("sh", "sz", "bj")) else codes[0]
        return [{"code": code, "last_price": 11, "pre_close_price": 10}]


def test_fetch_indicator_snapshots_splits_repeatedly_failing_large_batch():
    client = SplitClient()

    rows = fetch_indicator_snapshots(
        client,
        {
            "000001": {"name": "平安银行", "market": "SZ", "limit_up_threshold": 10},
            "000002": {"name": "万科A", "market": "SZ", "limit_up_threshold": 10},
        },
        batch_size=2,
        retry_attempts=0,
        retry_backoff_seconds=0,
        request_spacing_seconds=0,
        fallback_batch_size=1,
    )

    assert sorted(row["code"] for row in rows) == ["000001", "000002"]
    assert client.calls[0] == ["sz000001", "sz000002"]
    assert len(client.calls) == 3


def test_fetch_indicator_snapshots_isolates_one_failed_symbol_for_bounded_monitoring():
    class IsolatedFailureClient:
        def get_quote(self, codes):
            if "sz000004" in codes:
                raise RuntimeError("symbol unavailable")
            return [
                {"code": code[2:], "last_price": 11, "pre_close_price": 10}
                for code in codes
            ]

    rows = fetch_indicator_snapshots(
        IsolatedFailureClient(),
        {
            "000001": {"name": "平安银行", "market": "SZ", "limit_up_threshold": 10},
            "000002": {"name": "万科A", "market": "SZ", "limit_up_threshold": 10},
            "000004": {"name": "国华网安", "market": "SZ", "limit_up_threshold": 10},
        },
        requested_codes={"000001", "000002", "000004"},
        batch_size=240,
        retry_attempts=0,
        retry_backoff_seconds=0,
        request_spacing_seconds=0,
        fallback_batch_size=80,
        allow_partial_failures=True,
    )

    assert sorted(row["code"] for row in rows) == ["000001", "000002"]


def test_fetch_indicator_snapshots_uses_tracked_codes_without_universe_discovery():
    class TrackedClient:
        def get_a_share_codes_all(self):
            raise AssertionError("tracked captures must not rediscover the full market")

        def get_quote(self, codes):
            assert codes == ["sz000001"]
            return [{"code": "000001", "last_price": 11, "pre_close_price": 10}]

    rows = fetch_indicator_snapshots(
        TrackedClient(),
        {"000001": {"name": "平安银行", "market": "SZ", "limit_up_threshold": 10}},
        requested_codes={"000001"},
        captured_at=datetime(2026, 7, 28, 1, 42, tzinfo=timezone.utc),
        request_spacing_seconds=0,
    )

    assert [row["code"] for row in rows] == ["000001"]
