from __future__ import annotations

import copy
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from typing import Any, Callable


TaskMutator = Callable[[dict[str, Any]], None]


class MediaTranscriptionTaskStore:
    def __init__(self, max_workers: int = 2) -> None:
        self.executor = ThreadPoolExecutor(max_workers=max_workers, thread_name_prefix="media-transcription")
        self._tasks: dict[str, dict[str, Any]] = {}
        self._lock = threading.RLock()

    def create_task_record(self, task_id: str, file_name: str, source_url: str | None = None) -> dict[str, Any]:
        now = time.time()
        task = {
            "status": "queued",
            "transcript": "",
            "polished": "",
            "summary": "",
            "metadata": {},
            "file_name": file_name,
            "error": None,
            "created_at": now,
            "download_progress": {
                "phase": "pending",
                "progress": 0,
                "downloaded_bytes": 0,
                "total_bytes": 0,
                "eta_seconds": None,
                "speed_bytes_per_sec": 0,
            },
            "intake_progress": {
                "phase": "pending",
                "progress": 0,
                "processed_bytes": 0,
                "total_bytes": 0,
                "eta_seconds": None,
            },
            "qa_items": [],
        }
        if source_url:
            task["source_url"] = source_url
        with self._lock:
            self._tasks[task_id] = task
            return copy.deepcopy(task)

    def get_task(self, task_id: str) -> dict[str, Any] | None:
        with self._lock:
            task = self._tasks.get(task_id)
            return copy.deepcopy(task) if task is not None else None

    def update_task(self, task_id: str, **fields: Any) -> dict[str, Any] | None:
        with self._lock:
            task = self._tasks.get(task_id)
            if task is None:
                return None
            task.update(fields)
            return copy.deepcopy(task)

    def mutate_task(self, task_id: str, mutator: TaskMutator) -> dict[str, Any] | None:
        with self._lock:
            task = self._tasks.get(task_id)
            if task is None:
                return None
            mutator(task)
            return copy.deepcopy(task)
