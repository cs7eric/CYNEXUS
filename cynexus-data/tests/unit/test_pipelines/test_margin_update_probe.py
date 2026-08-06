from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.margin import update_probe
from cynexus_data.runtime.context import PipelineContext


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    postgres = FakePostgresHook()


class FakeRepo:
    latest = {"summary": date(2026, 7, 29), "detail": date(2026, 7, 29), "account": date(2026, 7, 2)}

    def __init__(self, client):
        self.client = client

    def create_table(self):
        pass

    def latest_trade_dates(self):
        return self.latest


def test_margin_update_probe_skips_when_stored_data_is_current(monkeypatch) -> None:
    monkeypatch.setattr(update_probe, "MarginRepository", FakeRepo)
    monkeypatch.setattr(update_probe.sync_margin, "_provider_clients", lambda context, providers: [("exchange", object())])
    monkeypatch.setattr(update_probe.sync_margin, "_latest_trade_date", lambda clients, end_date: date(2026, 7, 29))
    sync_calls = []
    monkeypatch.setattr(update_probe.sync_margin, "run", lambda *args, **kwargs: sync_calls.append(args) or {})

    result = update_probe.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(job_key="margin_update_probe", params={"create_table": False}),
    )

    assert result["status"] == "skipped"
    assert result["skip_reason"] == "already_current"
    assert sync_calls == []


def test_margin_update_probe_runs_sync_for_new_trade_date(monkeypatch) -> None:
    monkeypatch.setattr(update_probe, "MarginRepository", FakeRepo)
    monkeypatch.setattr(update_probe.sync_margin, "_provider_clients", lambda context, providers: [("exchange", object())])
    monkeypatch.setattr(update_probe.sync_margin, "_latest_trade_date", lambda clients, end_date: date(2026, 7, 30))
    sync_requests = []

    def fake_sync(context, request):
        sync_requests.append(request)
        return {"rows_read": 10, "rows_written": 5}

    monkeypatch.setattr(update_probe.sync_margin, "run", fake_sync)

    result = update_probe.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(job_key="margin_update_probe", trigger_request_id="probe-1", params={"create_table": False}),
    )

    assert result["status"] == "success"
    assert result["rows_written"] == 5
    assert sync_requests[0].params["trade_date"] == "2026-07-30"
    assert sync_requests[0].triggered_by == "margin_update_probe"


def test_margin_update_probe_skips_when_source_is_not_available(monkeypatch) -> None:
    monkeypatch.setattr(update_probe, "MarginRepository", FakeRepo)
    monkeypatch.setattr(update_probe.sync_margin, "_provider_clients", lambda context, providers: [("exchange", object())])
    monkeypatch.setattr(update_probe.sync_margin, "_latest_trade_date", lambda clients, end_date: None)

    result = update_probe.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(job_key="margin_update_probe", params={"create_table": False}),
    )

    assert result["status"] == "skipped"
    assert result["skip_reason"] == "source_not_updated"
