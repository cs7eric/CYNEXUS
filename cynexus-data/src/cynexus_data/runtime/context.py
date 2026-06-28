from __future__ import annotations

from dataclasses import dataclass, field

from cynexus_data.connections.registry import ConnectionRegistry
from cynexus_data.hooks.sources.akshare import AkshareHook
from cynexus_data.hooks.sources.eastmoney import EastMoneyHook
from cynexus_data.hooks.sources.sina import SinaHook
from cynexus_data.hooks.sources.tdx import TdxHook
from cynexus_data.hooks.sources.tencent import TencentHook
from cynexus_data.hooks.sources.ths import ThsHook
from cynexus_data.hooks.storage.clickhouse import ClickHouseHook
from cynexus_data.hooks.storage.duckdb import DuckDBHook
from cynexus_data.hooks.storage.postgres import PostgresHook
from cynexus_data.runtime.checkpoints import CheckpointStore
from cynexus_data.runtime.job_store import JobStore
from cynexus_data.runtime.locks import LockManager


@dataclass
class HookBundle:
    registry: ConnectionRegistry

    @property
    def clickhouse(self) -> ClickHouseHook:
        return ClickHouseHook("clickhouse_default", self.registry)

    @property
    def postgres(self) -> PostgresHook:
        return PostgresHook("postgres_default", self.registry)

    @property
    def duckdb(self) -> DuckDBHook:
        return DuckDBHook("duckdb_default", self.registry)

    @property
    def eastmoney(self) -> EastMoneyHook:
        return EastMoneyHook("eastmoney_default", self.registry)

    @property
    def akshare(self) -> AkshareHook:
        return AkshareHook("akshare_default", self.registry)

    @property
    def tencent(self) -> TencentHook:
        return TencentHook("tencent_default", self.registry)

    @property
    def sina(self) -> SinaHook:
        return SinaHook("sina_default", self.registry)

    @property
    def tdx(self) -> TdxHook:
        return TdxHook("tdx_local", self.registry)

    @property
    def ths(self) -> ThsHook:
        return ThsHook("ths_default", self.registry)


@dataclass
class PipelineContext:
    registry: ConnectionRegistry = field(default_factory=ConnectionRegistry)
    hooks: HookBundle | None = None
    job_store: JobStore = field(default_factory=JobStore.memory)
    checkpoints: CheckpointStore = field(default_factory=CheckpointStore.memory)
    locks: LockManager = field(default_factory=LockManager.memory)

    def __post_init__(self) -> None:
        if self.hooks is None:
            self.hooks = HookBundle(self.registry)

    @classmethod
    def default(cls, persist_job_state: bool = False) -> "PipelineContext":
        registry = ConnectionRegistry()
        if not persist_job_state:
            return cls(registry=registry)
        pg = PostgresHook("postgres_default", registry).get_client()
        return cls(
            registry=registry,
            job_store=JobStore.postgres(pg),
            checkpoints=CheckpointStore.postgres(pg),
            locks=LockManager.postgres(pg),
        )
