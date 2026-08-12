from pathlib import Path

from cynexus_data.sources.local.parquet_1m import Parquet1mFile, _prefer_parquet_1m_file


def _file(name: str, *, rows: int, mtime: float = 1.0) -> Parquet1mFile:
    return Parquet1mFile(
        path=Path(name),
        market="SH",
        symbol="600248",
        ts_code="600248.SH",
        rows=rows,
        size=1,
        mtime=mtime,
    )


def test_prefer_duplicate_export_with_more_rows() -> None:
    assert _prefer_parquet_1m_file(
        _file("600248.SH(1).parquet", rows=1_030_034),
        _file("600248.SH.parquet", rows=1_029_793),
    )


def test_prefer_canonical_name_when_duplicate_exports_have_equal_rows() -> None:
    assert _prefer_parquet_1m_file(
        _file("600248.SH.parquet", rows=1_030_034),
        _file("600248.SH(1).parquet", rows=1_030_034),
    )
