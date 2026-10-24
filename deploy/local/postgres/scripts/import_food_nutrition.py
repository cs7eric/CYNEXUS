"""Validate and transactionally import the offline food catalog through Docker psql.

Administrative bootstrap utility, not an application service or autonomous Agent
tool. Uses the existing PostgreSQL container authentication; no passwords or new
dependencies are required. Apply apply-life-migrations.ps1 before importing.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import subprocess
import tempfile
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path
from typing import Any


NUTRIENTS = {
    "caloriesKcal": "calories_kcal",
    "carbsGrams": "carbs_g",
    "proteinGrams": "protein_g",
    "fatGrams": "fat_g",
}


def stable_id(namespace: str, *parts: str) -> int:
    encoded = json.dumps([namespace, *parts], ensure_ascii=False).encode("utf-8")
    return (int.from_bytes(hashlib.sha256(encoded).digest()[:8], "big") & ((1 << 63) - 1)) or 1


def validate_json_text(value: Any) -> None:
    if isinstance(value, str):
        if "\x00" in value:
            raise ValueError("PostgreSQL JSONB cannot store NUL characters")
        value.encode("utf-8")
    elif isinstance(value, dict):
        for key, item in value.items():
            validate_json_text(key)
            validate_json_text(item)
    elif isinstance(value, list):
        for item in value:
            validate_json_text(item)


def validate_catalog(path: Path) -> tuple[dict[str, Any], dict[str, Any], list[dict[str, Any]]]:
    content = path.read_bytes()
    catalog = json.loads(content)
    records = catalog.get("records")
    if not isinstance(records, list) or not records:
        raise ValueError("Catalog must contain a nonempty records array")
    validate_json_text(catalog)
    checked_at = datetime.fromisoformat(catalog["checkedAt"].replace("Z", "+00:00"))
    if checked_at.utcoffset() is None:
        raise ValueError("checkedAt must include its timezone")
    identities: set[tuple[str, str, str]] = set()
    record_ids: set[int] = set()
    grouped: dict[tuple[str, str], list[dict[str, Any]]] = defaultdict(list)
    for index, record in enumerate(records):
        for key in ("sourceKey", "source", "sourceId", "description", "license", "usageScope"):
            if not isinstance(record.get(key), str) or not record[key].strip():
                raise ValueError(f"Record {index}: missing string {key}")
        identity = (record["sourceKey"], record["source"], record["sourceId"])
        row_id = stable_id("food", *identity)
        if identity in identities or row_id in record_ids:
            raise ValueError(f"Record {index}: duplicate identity or ID collision")
        identities.add(identity)
        record_ids.add(row_id)
        if record.get("basis") != {"amount": 100, "unit": "g"}:
            raise ValueError(f"Record {index}: only measured per-100-g foods are accepted")
        nutrients = record.get("per100g", {})
        if set(nutrients) != set(NUTRIENTS):
            raise ValueError(f"Record {index}: expected all four nullable nutrient fields")
        for value in nutrients.values():
            if value is not None and (
                isinstance(value, bool) or not isinstance(value, (int, float))
                or not math.isfinite(value) or value < 0
            ):
                raise ValueError(f"Record {index}: nutrient must be finite nonnegative numeric or null")
        complete = all(value is not None for value in nutrients.values())
        usable = any(value is not None for value in nutrients.values())
        if record.get("complete") is not complete or record.get("nutritionUsable") is not usable:
            raise ValueError(f"Record {index}: completeness flags do not match nutrient values")
        for key in ("aliases", "estimatedFields"):
            values = record.get(key, [])
            if not isinstance(values, list) or not all(isinstance(v, str) for v in values):
                raise ValueError(f"Record {index}: {key} must be a string array")
        if not set(record.get("estimatedFields", [])) <= set(NUTRIENTS):
            raise ValueError(f"Record {index}: unknown estimated nutrient field")
        if not isinstance(record.get("sample_only", False), bool):
            raise ValueError(f"Record {index}: sample_only must be boolean")
        if not isinstance(catalog.get("sources", {}).get(record["sourceKey"]), dict):
            raise ValueError(f"Record {index}: source manifest is missing")
        grouped[identity[:2]].append(record)

    sources = []
    source_ids: set[int] = set()
    for (source_key, dataset_name), rows in sorted(grouped.items()):
        if len({(r["license"], r["usageScope"], r.get("sample_only", False)) for r in rows}) != 1:
            raise ValueError(f"Inconsistent dataset license/scope/sample flags: {dataset_name}")
        source_id = stable_id("source", source_key, dataset_name)
        if source_id in source_ids:
            raise ValueError("Source ID collision")
        source_ids.add(source_id)
        sources.append({
            "id": source_id, "source_key": source_key, "dataset_name": dataset_name,
            "license": rows[0]["license"], "usage_scope": rows[0]["usageScope"],
            "sample_only": rows[0].get("sample_only", False),
            "metadata": {"sourceManifest": catalog["sources"][source_key],
                         "recordCount": len(rows),
                         "datasetReleases": sorted({r["datasetRelease"] for r in rows if r.get("datasetRelease")})},
        })
    counts = {"recordCount": len(records),
              "completeCount": sum(r["complete"] for r in records),
              "usableCount": sum(r["nutritionUsable"] for r in records)}
    for key, expected in counts.items():
        if catalog.get(key) != expected:
            raise ValueError(f"Manifest {key} does not match records")
    for key, field in (("countsBySource", "source"), ("countsByUsageScope", "usageScope")):
        if catalog.get(key) != dict(Counter(r[field] for r in records)):
            raise ValueError(f"Manifest {key} does not match records")
    digest = hashlib.sha256(content).hexdigest()
    batch = {
        "id": stable_id("batch", digest), "catalog_sha256": digest,
        "catalog_checked_at": catalog["checkedAt"],
        "record_count": counts["recordCount"], "complete_count": counts["completeCount"],
        "usable_count": counts["usableCount"], "source_count": len(sources),
        "manifest": {key: value for key, value in catalog.items() if key != "records"},
    }
    return catalog, batch, sources


IMPORT_SQL = """
INSERT INTO cynexus_appl_life.food_nutrition_source AS current
    (id, source_key, dataset_name, license, usage_scope, sample_only, metadata)
SELECT (payload->>'id')::bigint, payload->>'source_key', payload->>'dataset_name',
       payload->>'license', payload->>'usage_scope', (payload->>'sample_only')::boolean,
       payload->'metadata' FROM _life_sources
ON CONFLICT (source_key, dataset_name) DO UPDATE SET
    license=excluded.license, usage_scope=excluded.usage_scope,
    sample_only=excluded.sample_only, metadata=excluded.metadata,
    deleted_at=NULL, updated_at=now()
WHERE ROW(current.license,current.usage_scope,current.sample_only,current.metadata,current.deleted_at)
   IS DISTINCT FROM ROW(excluded.license,excluded.usage_scope,excluded.sample_only,excluded.metadata,NULL::timestamptz);

INSERT INTO cynexus_appl_life.food_nutrition_import_batch AS current
    (id, catalog_sha256, catalog_checked_at, record_count, complete_count, usable_count, source_count, manifest)
SELECT (payload->>'id')::bigint, payload->>'catalog_sha256',
       (payload->>'catalog_checked_at')::timestamptz,
       (payload->>'record_count')::integer, (payload->>'complete_count')::integer,
       (payload->>'usable_count')::integer, (payload->>'source_count')::integer,
       payload->'manifest' FROM _life_batch
ON CONFLICT (catalog_sha256) DO UPDATE SET deleted_at=NULL, updated_at=now()
WHERE current.deleted_at IS NOT NULL;

CREATE TEMP TABLE _life_rows ON COMMIT DROP AS
SELECT incoming.id, source.id AS source_id, payload->>'sourceId' AS source_food_id,
       payload->>'description' AS description,
       ARRAY(SELECT jsonb_array_elements_text(coalesce(payload->'aliases','[]'::jsonb))) AS aliases,
       100::numeric AS basis_amount, 'g'::text AS basis_unit,
       (payload->'per100g'->>'caloriesKcal')::numeric AS calories_kcal,
       (payload->'per100g'->>'carbsGrams')::numeric AS carbs_g,
       (payload->'per100g'->>'proteinGrams')::numeric AS protein_g,
       (payload->'per100g'->>'fatGrams')::numeric AS fat_g,
       (payload->>'complete')::boolean AS is_complete,
       (payload->>'nutritionUsable')::boolean AS nutrition_usable,
       ARRAY(SELECT jsonb_array_elements_text(coalesce(payload->'estimatedFields','[]'::jsonb))) AS estimated_fields,
       payload->>'license' AS license, payload->>'usageScope' AS usage_scope,
       batch.id AS import_batch_id, payload AS raw_record
FROM _life_foods incoming
JOIN cynexus_appl_life.food_nutrition_source source
  ON source.source_key=payload->>'sourceKey' AND source.dataset_name=payload->>'source'
CROSS JOIN cynexus_appl_life.food_nutrition_import_batch batch
WHERE batch.catalog_sha256=(SELECT payload->>'catalog_sha256' FROM _life_batch);

INSERT INTO cynexus_appl_life.food_nutrition_catalog AS current
    (id,source_id,source_food_id,description,aliases,basis_amount,basis_unit,
     calories_kcal,carbs_g,protein_g,fat_g,is_complete,nutrition_usable,estimated_fields,
     license,usage_scope,import_batch_id,raw_record)
SELECT * FROM _life_rows
ON CONFLICT (source_id,source_food_id) DO UPDATE SET
    description=excluded.description, aliases=excluded.aliases,
    basis_amount=excluded.basis_amount, basis_unit=excluded.basis_unit,
    calories_kcal=excluded.calories_kcal, carbs_g=excluded.carbs_g,
    protein_g=excluded.protein_g, fat_g=excluded.fat_g,
    is_complete=excluded.is_complete, nutrition_usable=excluded.nutrition_usable,
    estimated_fields=excluded.estimated_fields, license=excluded.license,
    usage_scope=excluded.usage_scope, import_batch_id=excluded.import_batch_id,
    raw_record=excluded.raw_record, deleted_at=NULL, updated_at=now()
WHERE ROW(current.description,current.aliases,current.basis_amount,current.basis_unit,
          current.calories_kcal,current.carbs_g,current.protein_g,current.fat_g,
          current.is_complete,current.nutrition_usable,current.estimated_fields,
          current.license,current.usage_scope,current.raw_record,current.import_batch_id,current.deleted_at)
   IS DISTINCT FROM ROW(excluded.description,excluded.aliases,excluded.basis_amount,excluded.basis_unit,
          excluded.calories_kcal,excluded.carbs_g,excluded.protein_g,excluded.fat_g,
          excluded.is_complete,excluded.nutrition_usable,excluded.estimated_fields,
          excluded.license,excluded.usage_scope,excluded.raw_record,excluded.import_batch_id,NULL::timestamptz);

DO $verify$
BEGIN
    IF (SELECT count(*) FROM _life_rows) <> (SELECT count(*) FROM _life_foods) THEN
        RAISE EXCEPTION 'Source or batch mapping lost incoming rows';
    END IF;
    IF EXISTS (
        SELECT 1 FROM _life_rows incoming
        LEFT JOIN cynexus_appl_life.food_nutrition_catalog stored
          ON stored.source_id=incoming.source_id AND stored.source_food_id=incoming.source_food_id
        WHERE stored.id IS NULL OR stored.deleted_at IS NOT NULL
           OR (to_jsonb(stored)-ARRAY['id','created_at','updated_at','deleted_at'])
              IS DISTINCT FROM (to_jsonb(incoming)-'id')
    ) THEN
        RAISE EXCEPTION 'Imported records failed full value/metadata verification';
    END IF;
END
$verify$;

WITH asset_names AS (
    SELECT DISTINCT a.catalog_key,
           lower(regexp_replace(btrim(n.name), '\\s+', ' ', 'g')) AS normalized_name
    FROM cynexus_appl_life.food_asset_mapping a
    CROSS JOIN LATERAL unnest(
        ARRAY[a.canonical_name, a.english_name] || COALESCE(a.aliases, '{}'::text[])
    ) AS n(name)
    WHERE a.deleted_at IS NULL AND n.name IS NOT NULL AND btrim(n.name) <> ''
),
food_names AS (
    SELECT DISTINCT c.id AS catalog_food_id,
           lower(regexp_replace(btrim(n.name), '\\s+', ' ', 'g')) AS normalized_name
    FROM cynexus_appl_life.food_nutrition_catalog c
    CROSS JOIN LATERAL (
        SELECT c.description AS name
        UNION ALL
        SELECT unnest(COALESCE(c.aliases, '{}'::text[])) AS name
    ) AS n
    WHERE c.deleted_at IS NULL AND n.name IS NOT NULL AND btrim(n.name) <> ''
),
candidate_assets AS (
    SELECT DISTINCT f.catalog_food_id, a.catalog_key
    FROM food_names f
    JOIN asset_names a USING (normalized_name)
),
unambiguous_matches AS (
    SELECT catalog_food_id, min(catalog_key) AS catalog_key
    FROM candidate_assets
    GROUP BY catalog_food_id
    HAVING count(DISTINCT catalog_key)=1
)
INSERT INTO cynexus_appl_life.food_asset_catalog_mapping
    (catalog_food_id,catalog_key,association_source)
SELECT catalog_food_id,catalog_key,'name_match'
FROM unambiguous_matches
ON CONFLICT (catalog_food_id) DO NOTHING;

SELECT jsonb_build_object(
    'schema','cynexus_appl_life', 'catalogSha256', b.catalog_sha256,
    'batchId', b.id::text, 'records',count(*),
    'complete',count(*) FILTER (WHERE f.is_complete),
    'usable',count(*) FILTER (WHERE f.nutrition_usable),
    'sources',count(DISTINCT f.source_id),
    'sampleOnlyRecords',count(*) FILTER (WHERE s.sample_only),
    'countsBySource',(SELECT jsonb_object_agg(name,n) FROM (
        SELECT source.dataset_name AS name,count(*) AS n FROM _life_rows r
        JOIN cynexus_appl_life.food_nutrition_source source ON source.id=r.source_id
        GROUP BY source.dataset_name) counts)
)
FROM _life_rows f JOIN cynexus_appl_life.food_nutrition_source s ON s.id=f.source_id
JOIN cynexus_appl_life.food_nutrition_import_batch b ON b.id=f.import_batch_id
GROUP BY b.id;
COMMIT;
"""


def write_import(stream: Any, catalog: dict[str, Any], batch: dict[str, Any], sources: list[dict[str, Any]]) -> None:
    stream.write("BEGIN;\nSET LOCAL lock_timeout='15s';\nSET LOCAL statement_timeout='180s';\n")
    stream.write("DO $$ BEGIN PERFORM pg_advisory_xact_lock(hashtextextended('cynexus_appl_life.food_nutrition_import',0)); END $$;\n")
    stream.write("CREATE TEMP TABLE _life_sources(payload jsonb) ON COMMIT DROP;\n")
    stream.write("CREATE TEMP TABLE _life_batch(payload jsonb) ON COMMIT DROP;\n")
    stream.write("CREATE TEMP TABLE _life_foods(id bigint,payload jsonb) ON COMMIT DROP;\n")
    writer = csv.writer(stream, lineterminator="\n")
    for table, rows in (("_life_sources", sources), ("_life_batch", [batch])):
        stream.write(f"COPY {table}(payload) FROM STDIN WITH (FORMAT csv);\n")
        for row in rows:
            writer.writerow([json.dumps(row, ensure_ascii=False, allow_nan=False)])
        stream.write("\\.\n")
    stream.write("COPY _life_foods(id,payload) FROM STDIN WITH (FORMAT csv);\n")
    for row in catalog["records"]:
        writer.writerow([stable_id("food", row["sourceKey"], row["source"], row["sourceId"]),
                         json.dumps(row, ensure_ascii=False, allow_nan=False)])
    stream.write("\\.\n")
    stream.write(IMPORT_SQL)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--container", default="cynexus-postgres")
    parser.add_argument("--database", default="cynexus")
    parser.add_argument("--user", default="postgres")
    parser.add_argument("--dry-run", action="store_true", help="Validate inputs without accessing PostgreSQL")
    parser.add_argument("--report", type=Path, help="Optional local JSON verification report")
    args = parser.parse_args()
    catalog, batch, sources = validate_catalog(args.catalog)
    if args.dry_run:
        report = {"dryRun": True, "catalogSha256": batch["catalog_sha256"],
                  "records": batch["record_count"], "complete": batch["complete_count"],
                  "usable": batch["usable_count"], "sources": len(sources)}
    else:
        # The temporary COPY stream is deleted on success and failure; no data dump
        # is checked in or left inside the database container.
        with tempfile.TemporaryDirectory(prefix="cynexus-food-import-") as temp:
            script_path = Path(temp) / "import.sql"
            with script_path.open("w", encoding="utf-8", newline="") as stream:
                write_import(stream, catalog, batch, sources)
            with script_path.open("rb") as stream:
                result = subprocess.run(
                    ["docker", "exec", "-i", args.container, "psql", "-X", "-qAt",
                     "-U", args.user, "-d", args.database, "-v", "ON_ERROR_STOP=1"],
                    stdin=stream, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=300,
                )
            if result.returncode:
                raise RuntimeError("Import rolled back: " + result.stderr.decode("utf-8", errors="replace"))
            report = json.loads(result.stdout.decode("utf-8").strip())
            expected = {"records": batch["record_count"], "complete": batch["complete_count"],
                        "usable": batch["usable_count"], "sources": len(sources)}
            if any(report.get(key) != value for key, value in expected.items()):
                raise RuntimeError("Database report does not match catalog")
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=True, indent=2))


if __name__ == "__main__":
    main()
