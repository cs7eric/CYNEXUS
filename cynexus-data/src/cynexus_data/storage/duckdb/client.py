from __future__ import annotations

from contextlib import contextmanager
from pathlib import Path
from typing import Iterator

import duckdb


class DuckDBClient:
    def __init__(self, path: str) -> None:
        self.path = Path(path)

    @contextmanager
    def connect(self, read_only: bool = False) -> Iterator[duckdb.DuckDBPyConnection]:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        con = duckdb.connect(str(self.path), read_only=read_only)
        try:
            yield con
        finally:
            con.close()

    def health(self) -> dict:
        with self.connect(read_only=False) as con:
            version = con.execute("SELECT version()").fetchone()[0]
        return {"ok": True, "database": "duckdb", "version": version, "path": str(self.path)}

