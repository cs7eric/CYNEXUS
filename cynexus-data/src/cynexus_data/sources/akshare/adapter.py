from __future__ import annotations


def dataframe_to_records(df) -> list[dict]:
    if df is None:
        return []
    return list(df.to_dict(orient="records"))

