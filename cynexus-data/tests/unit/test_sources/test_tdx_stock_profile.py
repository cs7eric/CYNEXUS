from __future__ import annotations

from cynexus_data.sources.tdx.stock_profile import fetch_stock_share_profile


class Profile:
    circulating_shares = 1_000_000_000
    total_shares = 1_200_000_000


class Table:
    rows = [Profile()]


class Helpers:
    def __init__(self):
        self.calls = []

    def stock_profile_table(self, full_code):
        self.calls.append(full_code)
        return Table()


class Client:
    def __init__(self):
        self.helpers = Helpers()


def test_fetch_stock_share_profile_normalizes_symbol() -> None:
    client = Client()
    profile = fetch_stock_share_profile(client, "600000")

    assert client.helpers.calls == ["sh600000"]
    assert profile["circulating_shares"] == 1_000_000_000
    assert profile["total_shares"] == 1_200_000_000
