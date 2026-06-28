from __future__ import annotations

from cynexus_data.domain.market.index_codes import (
    all_index_codes,
    get_concept_codes,
    get_industry_codes,
    get_tdx_industry_56,
    get_tdx_industry_name,
    infer_index_kind,
    lookup_index_name,
    normalize_index_code,
    tdx_industry_seed_for,
)


def test_index_code_catalog_contains_legacy_industry_and_concept_codes() -> None:
    industry = dict(get_industry_codes())
    concept = dict(get_concept_codes())

    assert industry["sh880323"] == "银行"
    assert concept["sh880417"] == "人工智能"
    assert len(industry) >= 32
    assert len(concept) >= 40


def test_normalize_and_infer_index_kind() -> None:
    assert normalize_index_code("880323") == "sh880323"
    assert normalize_index_code("sz399001") == "sz399001"
    assert infer_index_kind("880323") == "industry"
    assert infer_index_kind("880417") == "concept"
    assert infer_index_kind("sh881001") == "sector"
    assert infer_index_kind("000001") == "unknown"


def test_lookup_and_extra_items() -> None:
    extras = [{"code": "sh881999", "name": "自定义板块", "kind": "sector"}]
    rows = all_index_codes(extra_items=extras)

    assert lookup_index_name("880323") == "银行"
    assert lookup_index_name("sh881999", extra_items=extras) == "自定义板块"
    assert ("sh881999", "自定义板块", "sector") in rows


def test_tdx_industry_56_catalog_and_seeds() -> None:
    catalog = get_tdx_industry_56()

    assert len(catalog) == 56
    assert catalog["880471"] == "银行"
    assert get_tdx_industry_name("sh880493") == "软件服务"
    assert tdx_industry_seed_for("880471") == ["sh601398", "sh600036"]
