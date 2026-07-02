from __future__ import annotations

from typing import Any

from cynexus_data.runtime.context import PipelineContext


def mark_parent_step_progress(
    context: PipelineContext,
    params: dict[str, Any],
    *,
    processed: int,
    total: int,
    rows_read: int,
    rows_written: int,
    message: str | None = None,
    unit_label: str | None = None,
    force: bool = False,
) -> None:
    run_id = params.get("progress_run_id")
    if not run_id:
        return
    every = int(params.get("progress_update_every_items") or params.get("progress_update_every_files") or 100)
    if not force and processed > 0 and processed % every != 0:
        return
    base_metadata = dict(params.get("progress_base_metadata") or {})
    parent_rows_read = int(params.get("progress_parent_rows_read") or 0)
    parent_rows_written = int(params.get("progress_parent_rows_written") or 0)
    step_code = str(params.get("progress_step_code") or base_metadata.get("lastStep") or "pipeline")
    resolved_unit_label = str(unit_label or params.get("progress_unit_label") or "项")
    progress = {
        "step": step_code,
        "unitLabel": resolved_unit_label,
        "processedItems": processed,
        "totalItems": total,
        "rowsRead": rows_read,
        "rowsWritten": rows_written,
    }
    if resolved_unit_label == "文件":
        progress["processedFiles"] = processed
        progress["totalFiles"] = total
    base_metadata.update(
        {
            "lastStep": step_code,
            "lastMessage": message
            or f"running {step_code}: {resolved_unit_label} {processed}/{total}, rows_read={rows_read}, rows_written={rows_written}",
            "currentStepProgress": progress,
            "rows_read": parent_rows_read + rows_read,
            "rows_written": parent_rows_written + rows_written,
        }
    )
    context.job_store.mark_progress(
        str(run_id),
        {
            "status": "running",
            "success": False,
            "rows_read": parent_rows_read + rows_read,
            "rows_written": parent_rows_written + rows_written,
            "metadata": base_metadata,
        },
    )
