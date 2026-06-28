from __future__ import annotations

import struct

from cynexus_data.sources.tdx.local_day import discover_day_files, read_tdx_day_file


def _write_day(path, records):
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = b"".join(
        struct.pack("<IIIIIfII", trade_date, int(open_ * 1000), int(high * 1000), int(low * 1000), int(close * 1000), amount, volume, 0)
        for trade_date, open_, high, low, close, amount, volume in records
    )
    path.write_bytes(payload)


def test_discover_and_read_tdx_day_file(tmp_path) -> None:
    day_path = tmp_path / "vipdoc" / "sz" / "lday" / "sz000001.day"
    _write_day(
        day_path,
        [
            (20260625, 10.0, 11.0, 9.0, 10.5, 1000.0, 100),
            (20260626, 10.5, 12.0, 10.0, 11.0, 2000.0, 200),
        ],
    )

    [file] = discover_day_files(tmp_path, markets=["sz"])
    frame = read_tdx_day_file(file, start_date="2026-06-26")

    assert file.market == "SZ"
    assert file.symbol == "000001"
    assert frame["symbol"].tolist() == ["000001"]
    assert frame["close"].tolist() == [11.0]
    assert frame["source"].tolist() == ["tdx_day"]
