from cynexus_data.domain.market.etf_mapping import build_etf_mapping_rows


def _keys(rows: list[dict[str, object]], group: str) -> set[str]:
    return {str(row["mapping_code"]) for row in rows if row["mapping_group"] == group}


def test_mapping_is_many_to_many_across_index_style_and_theme() -> None:
    rows = build_etf_mapping_rows("510300", "沪深300红利低波ETF")

    assert "broad" in _keys(rows, "universe")
    assert "csi_300" in _keys(rows, "index_scope")
    assert "dividend" in _keys(rows, "style")
    assert "low_volatility" in _keys(rows, "style")
    assert "china_a" in _keys(rows, "region")


def test_mapping_keeps_overseas_asset_class_separate_from_domestic_industry() -> None:
    rows = build_etf_mapping_rows("159920", "恒生医药ETF")

    assert "hong_kong" in _keys(rows, "region")
    assert "medicine" in _keys(rows, "theme")
    assert "equity" in _keys(rows, "asset_class")
