from __future__ import annotations

from contextlib import contextmanager
from datetime import datetime
from pathlib import Path
from typing import Any, Iterator

from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker


class PostgresClient:
    def __init__(self, url: str, schema: str = "cynexus_appl_market") -> None:
        self.url = url
        self.schema = schema
        self.engine: Engine = create_engine(url, pool_pre_ping=True, future=True)
        self.SessionLocal = sessionmaker(bind=self.engine, autoflush=False, autocommit=False, expire_on_commit=False, future=True)

    @contextmanager
    def session(self) -> Iterator[Session]:
        db = self.SessionLocal()
        try:
            yield db
            db.commit()
        except Exception:
            db.rollback()
            raise
        finally:
            db.close()

    def health(self) -> dict[str, Any]:
        with self.engine.connect() as conn:
            version = conn.execute(text("select version()")).scalar_one()
        return {"ok": True, "database": "postgres", "version": str(version)}

    def apply_sql_file(self, path: Path) -> None:
        sql = path.read_text(encoding="utf-8")
        with self.engine.begin() as conn:
            conn.execute(text(sql))

    def find_job_run_by_trigger(self, job_key: str, trigger_request_id: str) -> dict[str, Any] | None:
        with self.session() as db:
            row = db.execute(
                text(f"SELECT * FROM {self.schema}.task_job_run WHERE job_key=:job_key AND trigger_request_id=:trigger_request_id LIMIT 1"),
                {"job_key": job_key, "trigger_request_id": trigger_request_id},
            ).mappings().first()
            return dict(row) if row else None

    def get_job_run(self, run_id: str) -> dict[str, Any] | None:
        with self.session() as db:
            row = db.execute(text(f"SELECT * FROM {self.schema}.task_job_run WHERE run_id=:run_id"), {"run_id": run_id}).mappings().first()
            return dict(row) if row else None

    def upsert_job_run(self, record: dict[str, Any]) -> None:
        payload = _jsonable_record(record)
        with self.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.schema}.task_job_run (
                        run_id, job_key, trigger_request_id, triggered_by, dry_run, status, success,
                        started_at, finished_at, rows_read, rows_written, warnings, errors, params, metadata, updated_at
                    ) VALUES (
                        :run_id, :job_key, :trigger_request_id, :triggered_by, :dry_run, :status, :success,
                        :started_at, :finished_at, :rows_read, :rows_written, CAST(:warnings AS jsonb),
                        CAST(:errors AS jsonb), CAST(:params AS jsonb), CAST(:metadata AS jsonb), now()
                    )
                    ON CONFLICT (run_id) DO UPDATE SET
                        status = EXCLUDED.status,
                        success = EXCLUDED.success,
                        finished_at = EXCLUDED.finished_at,
                        rows_read = EXCLUDED.rows_read,
                        rows_written = EXCLUDED.rows_written,
                        warnings = EXCLUDED.warnings,
                        errors = EXCLUDED.errors,
                        metadata = EXCLUDED.metadata,
                        updated_at = now()
                    """
                ),
                payload,
            )

    def get_checkpoint(self, key: str) -> dict[str, Any] | None:
        with self.session() as db:
            row = db.execute(
                text(f"SELECT checkpoint_value FROM {self.schema}.task_checkpoint WHERE checkpoint_key=:key"),
                {"key": key},
            ).scalar_one_or_none()
            return row if isinstance(row, dict) else None

    def upsert_checkpoint(self, key: str, job_key: str, value: dict[str, Any]) -> None:
        import json

        with self.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.schema}.task_checkpoint (checkpoint_key, job_key, checkpoint_value, updated_at)
                    VALUES (:key, :job_key, CAST(:value AS jsonb), now())
                    ON CONFLICT (checkpoint_key) DO UPDATE SET
                        job_key = EXCLUDED.job_key,
                        checkpoint_value = EXCLUDED.checkpoint_value,
                        updated_at = now()
                    """
                ),
                {"key": key, "job_key": job_key, "value": json.dumps(value, ensure_ascii=False)},
            )

    def try_acquire_lock(self, lock_key: str, owner: str, expires_at: datetime) -> bool:
        with self.session() as db:
            row = db.execute(
                text(
                    f"""
                    INSERT INTO {self.schema}.task_job_lock (lock_key, owner, expires_at)
                    VALUES (:lock_key, :owner, :expires_at)
                    ON CONFLICT (lock_key) DO UPDATE SET
                        owner = EXCLUDED.owner,
                        expires_at = EXCLUDED.expires_at,
                        acquired_at = now()
                    WHERE {self.schema}.task_job_lock.expires_at < now()
                    RETURNING lock_key
                    """
                ),
                {"lock_key": lock_key, "owner": owner, "expires_at": expires_at},
            ).first()
            return row is not None

    def release_lock(self, lock_key: str, owner: str) -> None:
        with self.session() as db:
            db.execute(text(f"DELETE FROM {self.schema}.task_job_lock WHERE lock_key=:lock_key AND owner=:owner"), {"lock_key": lock_key, "owner": owner})

    def refresh_lock(self, lock_key: str, owner: str, expires_at: datetime) -> bool:
        with self.session() as db:
            row = db.execute(
                text(
                    f"""
                    UPDATE {self.schema}.task_job_lock
                       SET expires_at = :expires_at
                     WHERE lock_key = :lock_key AND owner = :owner
                    RETURNING lock_key
                    """
                ),
                {"lock_key": lock_key, "owner": owner, "expires_at": expires_at},
            ).first()
            return row is not None

    def mark_interrupted_running_jobs(self, message: str, job_keys: set[str] | None = None) -> int:
        job_clause = ""
        params: dict[str, Any] = {"message": message}
        if job_keys:
            placeholders = []
            for index, job_key in enumerate(sorted(job_keys)):
                name = f"job_key_{index}"
                placeholders.append(f":{name}")
                params[name] = job_key
            job_clause = f" AND job_key IN ({', '.join(placeholders)})"
        with self.session() as db:
            rows = db.execute(
                text(
                    f"""
                    UPDATE {self.schema}.task_job_run
                       SET status = 'expired',
                           success = false,
                           finished_at = now(),
                           errors = jsonb_build_array(CAST(:message AS text)),
                           metadata = COALESCE(metadata, '{{}}'::jsonb)
                               || jsonb_build_object('lastMessage', CAST(:message AS text), 'interrupted', true),
                           updated_at = now()
                     WHERE status = 'running'
                       AND finished_at IS NULL
                       {job_clause}
                    RETURNING run_id
                    """
                ),
                params,
            ).mappings().all()
            run_ids = [str(row["run_id"]) for row in rows]
            for run_id in run_ids:
                db.execute(text(f"DELETE FROM {self.schema}.task_job_lock WHERE owner=:owner"), {"owner": run_id})
            return len(run_ids)


def _jsonable_record(record: dict[str, Any]) -> dict[str, Any]:
    import json

    return {
        "run_id": record.get("run_id"),
        "job_key": record.get("job_key"),
        "trigger_request_id": record.get("trigger_request_id"),
        "triggered_by": record.get("triggered_by"),
        "dry_run": bool(record.get("dry_run", False)),
        "status": record.get("status", "running"),
        "success": bool(record.get("success", False)),
        "started_at": record.get("started_at"),
        "finished_at": record.get("finished_at"),
        "rows_read": int(record.get("rows_read") or 0),
        "rows_written": int(record.get("rows_written") or 0),
        "warnings": json.dumps(record.get("warnings") or [], ensure_ascii=False, default=str),
        "errors": json.dumps(record.get("errors") or [], ensure_ascii=False, default=str),
        "params": json.dumps(record.get("params") or {}, ensure_ascii=False, default=str),
        "metadata": json.dumps(record.get("metadata") or {}, ensure_ascii=False, default=str),
    }
