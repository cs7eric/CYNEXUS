from __future__ import annotations

import json
import os
from pathlib import Path
import sys
import urllib.request


ROOT = Path(__file__).resolve().parents[1]
DATASET = ROOT / "src" / "test" / "resources" / "market-information" / "evaluation-60.json"
DISCLAIMER = "Information analysis only; not investment advice."


def main() -> int:
    if os.getenv("CYNEXUS_MARKET_INFORMATION_LIVE_EVAL", "").lower() != "true":
        print("Live evaluation disabled; set CYNEXUS_MARKET_INFORMATION_LIVE_EVAL=true explicitly.")
        return 0
    api_key = os.getenv("CYNEXUS_AI_CUSTOM_API_KEY", "").strip()
    if not api_key:
        print("CYNEXUS_AI_CUSTOM_API_KEY is required", file=sys.stderr)
        return 2
    cases = json.loads(DATASET.read_text(encoding="utf-8"))
    smoke = _run(cases[:5], api_key, stop_after_failures=3)
    if smoke["consecutiveFailures"]:
        print(json.dumps(smoke, ensure_ascii=False, indent=2))
        return 1
    result = _run(cases, api_key, stop_after_failures=3)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if _accepted(result) else 1


def _run(cases: list[dict], api_key: str, *, stop_after_failures: int) -> dict:
    counters = {"total": 0, "schema": 0, "evidence": 0, "disclaimer": 0, "eventType": 0, "consecutiveFailures": 0}
    for case in cases:
        try:
            result = _complete(case, api_key)
            counters["total"] += 1
            counters["schema"] += int(_schema_ok(result))
            counters["evidence"] += int(_evidence_ok(result, case["content"]))
            counters["disclaimer"] += int(result.get("disclaimer") == DISCLAIMER)
            counters["eventType"] += int(result.get("eventType") == case["expected"]["eventType"])
            counters["consecutiveFailures"] = 0
        except Exception as exc:  # noqa: BLE001 - live harness must stop deterministically.
            counters["consecutiveFailures"] += 1
            counters["lastError"] = f"{type(exc).__name__}: {exc}"
            if counters["consecutiveFailures"] >= stop_after_failures:
                break
    return counters


def _complete(case: dict, api_key: str) -> dict:
    prompt = (
        "Return JSON only for this market event with fields summary,eventType,affectedScopes,entities,"
        "marketDirection,materiality,timeHorizon,novelty,confidence,evidence,risks,uncertainties,disclaimer. "
        "Evidence quote must be an exact substring. Disclaimer must be: " + DISCLAIMER + "\n"
        + json.dumps(case, ensure_ascii=False)
    )
    payload = json.dumps({
        "model": "gpt-5.6-sol", "input": prompt, "store": False,
        "reasoning": {"effort": "high"}, "text": {"format": {"type": "json_object"}},
    }).encode("utf-8")
    request = urllib.request.Request("https://yancc.cloud/v1/responses", data=payload, method="POST", headers={
        "Authorization": f"Bearer {api_key}", "Content-Type": "application/json",
    })
    with urllib.request.urlopen(request, timeout=300) as response:
        root = json.load(response)
    text = root.get("output_text") or next(
        part["text"] for item in root.get("output", []) for part in item.get("content", []) if part.get("text")
    )
    return json.loads(text)


def _schema_ok(value: dict) -> bool:
    required = {"summary", "eventType", "affectedScopes", "entities", "marketDirection", "materiality", "timeHorizon", "novelty", "confidence", "evidence", "risks", "uncertainties", "disclaimer"}
    return required.issubset(value) and isinstance(value.get("evidence"), list)


def _evidence_ok(value: dict, content: str) -> bool:
    evidence = value.get("evidence") or []
    return bool(evidence) and all(item.get("quote") and item["quote"] in content for item in evidence)


def _accepted(value: dict) -> bool:
    total = value.get("total") or 1
    return value.get("schema") == total and value.get("disclaimer") == total and value.get("evidence", 0) / total >= 0.95 and value.get("eventType", 0) / total >= 0.85


if __name__ == "__main__":
    raise SystemExit(main())
