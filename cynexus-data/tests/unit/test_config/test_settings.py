from __future__ import annotations

from cynexus_data.config.settings import resolve_env_template


def test_resolve_env_template_recurses_nested_defaults(monkeypatch) -> None:
    monkeypatch.delenv("MARKET_DATABASE_URL", raising=False)
    monkeypatch.setenv("DATABASE_URL", "postgresql+psycopg://postgres:secret@127.0.0.1:25433/cynexus")

    value = resolve_env_template("${MARKET_DATABASE_URL:${DATABASE_URL:postgresql+psycopg://postgres@127.0.0.1:25433/cynexus}}")

    assert value == "postgresql+psycopg://postgres:secret@127.0.0.1:25433/cynexus"
