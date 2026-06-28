from __future__ import annotations

from pathlib import Path

from cynexus_data.sources.tdx.parser import DayParseResult, parse_day_file


def parse_tdx_day_to_frame(path: Path, unit_scale: int = 1) -> DayParseResult:
    return parse_day_file(path, unit_scale=unit_scale)
