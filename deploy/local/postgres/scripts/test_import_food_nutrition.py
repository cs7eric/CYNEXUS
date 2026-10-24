"""Offline catalog validation and PostgreSQL COPY transport contract tests."""

import copy
import csv
import hashlib
import importlib.util
import io
import json
from collections import Counter
from pathlib import Path

import pytest


SPEC = importlib.util.spec_from_file_location(
    "food_import", Path(__file__).with_name("import_food_nutrition.py")
)
food_import = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(food_import)


def record(source_id="food-1", **changes):
    row = {
        "sourceKey": "fixture", "source": "Measured foods", "sourceId": source_id,
        "description": "Rice", "license": "CC0", "usageScope": "unrestricted",
        "basis": {"amount": 100, "unit": "g"},
        "per100g": {"caloriesKcal": 120, "carbsGrams": 25, "proteinGrams": 2.5, "fatGrams": 0},
        "complete": True, "nutritionUsable": True,
        "aliases": [], "estimatedFields": [], "sample_only": False,
    }
    row.update(changes)
    return row


def catalog_for(rows):
    return {
        "checkedAt": "2026-10-19T00:00:00Z",
        "sources": {r["sourceKey"]: {"url": "https://example.test/catalog"} for r in rows},
        "records": rows, "recordCount": len(rows),
        "completeCount": sum(r["complete"] is True for r in rows),
        "usableCount": sum(r["nutritionUsable"] is True for r in rows),
        "countsBySource": dict(Counter(r["source"] for r in rows)),
        "countsByUsageScope": dict(Counter(r["usageScope"] for r in rows)),
    }


def validate(tmp_path, catalog):
    path = tmp_path / "catalog.json"
    path.write_text(json.dumps(catalog, ensure_ascii=False), encoding="utf-8")
    return food_import.validate_catalog(path)


def copy_payloads(sql, table):
    """Decode generated CSV as COPY CSV does before JSONB parses its field."""
    start = sql.index(f"COPY {table}(")
    payload = sql[start:].split("\n", 1)[1].split("\n\\.\n", 1)[0]
    return list(csv.reader(io.StringIO(payload, newline="")))


def test_duplicate_identity_is_rejected_even_with_different_description(tmp_path):
    with pytest.raises(ValueError, match="duplicate identity"):
        validate(tmp_path, catalog_for([record(), record(description="Different name")]))


@pytest.mark.parametrize("value", [True, False, "0", [], {}, -0.01, float("nan"), float("inf"), -float("inf")])
def test_invalid_nutrients_cannot_reach_copy(tmp_path, value):
    row = record()
    row["per100g"]["fatGrams"] = value
    with pytest.raises(ValueError, match="finite nonnegative numeric"):
        validate(tmp_path, catalog_for([row]))


@pytest.mark.parametrize("basis", [
    {"amount": 100, "unit": "ml"}, {"amount": 1, "unit": "g"},
    {"amount": "100", "unit": "g"}, {"amount": 100, "unit": "G"}, None,
])
def test_servings_and_incompatible_units_are_rejected(tmp_path, basis):
    with pytest.raises(ValueError, match="per-100-g"):
        validate(tmp_path, catalog_for([record(basis=basis)]))


@pytest.mark.parametrize("changes", [
    {"complete": False}, {"nutritionUsable": False}, {"complete": 1},
    {"nutritionUsable": "true"}, {"sample_only": 1},
    {"estimatedFields": ["sodiumGrams"]}, {"aliases": "米饭"},
])
def test_invalid_quality_flags_and_metadata_are_rejected(tmp_path, changes):
    with pytest.raises(ValueError):
        validate(tmp_path, catalog_for([record(**changes)]))


@pytest.mark.parametrize("field", ["recordCount", "completeCount", "usableCount", "countsBySource", "countsByUsageScope"])
def test_manifest_cannot_misrepresent_records(tmp_path, field):
    catalog = catalog_for([record()])
    catalog[field] = {} if isinstance(catalog[field], dict) else 99
    with pytest.raises(ValueError, match=f"Manifest {field}"):
        validate(tmp_path, catalog)


def test_missing_nutrient_is_not_silently_interpreted_as_unknown(tmp_path):
    row = record()
    del row["per100g"]["fatGrams"]
    with pytest.raises(ValueError, match="four nullable nutrient"):
        validate(tmp_path, catalog_for([row]))


def test_actual_nul_is_rejected_before_postgresql(tmp_path):
    with pytest.raises(ValueError, match="NUL"):
        validate(tmp_path, catalog_for([record(aliases=["米\x00饭"])]))


def test_literal_unicode_escape_is_preserved_as_text(tmp_path):
    row = record(aliases=[r"\u0000", r"C:\foods\u0000.json"])
    catalog, batch, sources = validate(tmp_path, catalog_for([row]))
    stream = io.StringIO()
    food_import.write_import(stream, catalog, batch, sources)
    assert json.loads(copy_payloads(stream.getvalue(), "_life_foods")[0][1]) == row


@pytest.mark.parametrize("changes", [
    {"license": "restricted"}, {"usageScope": "evaluation"}, {"sample_only": True},
])
def test_shared_dataset_cannot_hide_conflicting_reuse_restrictions(tmp_path, changes):
    with pytest.raises(ValueError, match="Inconsistent dataset"):
        validate(tmp_path, catalog_for([record("a"), record("b", **changes)]))


def test_hash_collision_is_rejected_instead_of_overwriting_food(tmp_path, monkeypatch):
    monkeypatch.setattr(food_import, "stable_id", lambda *parts: 42)
    with pytest.raises(ValueError, match="ID collision"):
        validate(tmp_path, catalog_for([record("a"), record("b")]))


def test_null_zero_and_unicode_survive_copy_with_provenance(tmp_path):
    all_unknown = {key: None for key in food_import.NUTRIENTS}
    rows = [
        record("unknown", per100g=all_unknown, complete=False, nutritionUsable=False),
        record("zero", per100g=dict.fromkeys(food_import.NUTRIENTS, 0)),
        record("mixed", per100g={**all_unknown, "fatGrams": 0}, complete=False,
               description='米饭 "熟"\n第二行\\原始', aliases=['米,饭', '"别名"', '行\n尾', '\\.', '路径\\原样'],
               estimatedFields=["fatGrams"]),
    ]
    catalog, batch, sources = validate(tmp_path, catalog_for(rows))
    stream = io.StringIO(newline="")
    food_import.write_import(stream, catalog, batch, sources)
    sql = stream.getvalue()
    transported = copy_payloads(sql, "_life_foods")
    assert [json.loads(row[1]) for row in transported] == rows
    assert json.loads(transported[0][1])["per100g"]["fatGrams"] is None
    assert json.loads(transported[1][1])["per100g"]["fatGrams"] == 0
    assert (batch["record_count"], batch["complete_count"], batch["usable_count"]) == (3, 1, 2)
    assert [json.loads(row[0]) for row in copy_payloads(sql, "_life_sources")] == sources
    assert json.loads(copy_payloads(sql, "_life_batch")[0][0]) == batch
    assert "米饭" in sql


def test_identity_ids_remain_stable_across_order_and_content_changes(tmp_path):
    first = catalog_for([record("a"), record("b")])
    first_catalog, first_batch, first_sources = validate(tmp_path, first)
    first_stream = io.StringIO()
    food_import.write_import(first_stream, first_catalog, first_batch, first_sources)
    changed = copy.deepcopy(first)
    changed["records"].reverse()
    changed["records"][0]["description"] = "Updated label"
    second_catalog, second_batch, second_sources = validate(tmp_path, changed)
    second_stream = io.StringIO()
    food_import.write_import(second_stream, second_catalog, second_batch, second_sources)
    def ids_by_source_id(stream):
        return {json.loads(row[1])["sourceId"]: row[0]
                for row in copy_payloads(stream.getvalue(), "_life_foods")}
    assert ids_by_source_id(first_stream) == ids_by_source_id(second_stream)
    assert first_sources[0]["id"] == second_sources[0]["id"]
    assert first_batch["id"] != second_batch["id"]
    for row in first["records"]:
        identity = (row["sourceKey"], row["source"], row["sourceId"])
        expected = food_import.stable_id("food", *identity)
        assert 0 < expected < 2**63
        assert expected == food_import.stable_id("food", *identity)
        assert expected != food_import.stable_id("source", *identity)
    # Tuple boundaries must survive concatenation; upstream IDs can contain punctuation.
    assert food_import.stable_id("food", "ab", "c") != food_import.stable_id("food", "a", "bc")
    raw = (tmp_path / "catalog.json").read_bytes()
    assert second_batch["catalog_sha256"] == hashlib.sha256(raw).hexdigest()


def test_same_upstream_id_in_distinct_datasets_remains_distinct(tmp_path):
    rows = [record(), record(source="Other release", sample_only=True)]
    catalog, batch, sources = validate(tmp_path, catalog_for(rows))
    stream = io.StringIO()
    food_import.write_import(stream, catalog, batch, sources)
    ids = [row[0] for row in copy_payloads(stream.getvalue(), "_life_foods")]
    assert len(set(ids)) == 2
    assert len(sources) == 2
    assert {s["sample_only"] for s in sources} == {True, False}


def test_import_updates_exact_unambiguous_artwork_associations_without_replacing_manual_links():
    sql = food_import.IMPORT_SQL

    assert sql.index("INSERT INTO cynexus_appl_life.food_asset_catalog_mapping") > sql.index("$verify$;")
    assert "HAVING count(DISTINCT catalog_key)=1" in sql
    assert "SELECT catalog_food_id,catalog_key,'name_match'" in sql
    assert "ON CONFLICT (catalog_food_id) DO NOTHING" in sql
