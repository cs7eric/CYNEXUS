from cynexus_data.storage.postgres.ids import stable_bigint_id
from cynexus_data.storage.postgres.stock_sector import StockSectorRepository


class FakeSession:
    def __init__(self):
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, statement, params=None):
        self.calls.append((str(statement), params))


class FakeClient:
    schema = "cynexus_appl_market"

    def __init__(self):
        self.session_obj = FakeSession()

    def session(self):
        return self.session_obj


def test_stock_sector_ids_are_stable_business_keys() -> None:
    mapping = {"sector_id": "2:651", "category_raw": 2, "category_label": "行业", "name": "有色金属"}
    client = FakeClient()

    assert StockSectorRepository(client).replace_stock_mappings("002155", [mapping]) == 1

    sector_params = client.session_obj.calls[1][1]
    relation_params = client.session_obj.calls[2][1]
    assert sector_params["id"] == stable_bigint_id("sec_sector", "2:651")
    assert relation_params["id"] == stable_bigint_id("sec_stock_sector", "002155", "2:651")
