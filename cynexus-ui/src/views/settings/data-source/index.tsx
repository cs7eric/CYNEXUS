import { useCallback, useEffect, useMemo, useState } from "react"
import { AlertTriangle, Database, RefreshCcw, Search } from "lucide-react"

import DogLoader from "@/components/loader/dog-loader"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { WorkspaceShell } from "@/layout/workspace-shell"
import {
  type DataSourceFreshnessItem,
  type DataSourceFreshnessResponse,
  type DataSourceStatus,
  fetchDataSourceFreshness,
} from "@/services/market/data-source"
import { FreshnessSummary } from "./components/freshness-summary"
import { FreshnessTable } from "./components/freshness-table"
import { formatDateTime } from "./lib/format"

type SourceFilter = "all" | "postgres" | "clickhouse"
type StatusFilter = "all" | DataSourceStatus

const REFRESH_INTERVAL_MS = 30_000

export default function DataSourceSettingsPage() {
  const [data, setData] = useState<DataSourceFreshnessResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>("all")
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all")

  const refresh = useCallback(async (showLoading = true) => {
    if (showLoading) setLoading(true)
    setError(null)
    try {
      setData(await fetchDataSourceFreshness())
    } catch (err) {
      setError(err instanceof Error ? err.message : "数据源状态拉取失败")
    } finally {
      if (showLoading) setLoading(false)
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    fetchDataSourceFreshness()
      .then((payload) => {
        if (!cancelled) {
          setData(payload)
          setError(null)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "数据源状态拉取失败")
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })
    const timer = window.setInterval(() => void refresh(false), REFRESH_INTERVAL_MS)
    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [refresh])

  const items = useMemo(() => {
    const keyword = query.trim().toLowerCase()
    return (data?.items || []).filter((item) => {
      const matchesSource = sourceFilter === "all" || item.sourceType === sourceFilter
      const matchesStatus = statusFilter === "all" || item.status === statusFilter
      const haystack = [
        item.tableName,
        item.schemaName,
        item.databaseName,
        item.effectiveDateColumn,
        item.businessLogic,
        item.updateJob,
        item.dataSource,
        ...(item.usedBy || []),
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
      return matchesSource && matchesStatus && (!keyword || haystack.includes(keyword))
    })
  }, [data?.items, query, sourceFilter, statusFilter])

  const statusCounts = useMemo(() => {
    const base: Record<StatusFilter, number> = {
      all: data?.summary.total || 0,
      fresh: data?.summary.fresh || 0,
      lagging: data?.summary.lagging || 0,
      stale: data?.summary.stale || 0,
      empty: data?.summary.empty || 0,
      untracked: data?.summary.untracked || 0,
      error: data?.summary.error || 0,
    }
    return base
  }, [data?.summary])

  return (
    <WorkspaceShell sectionLabel="Settings" pageTitle="Data Source">
      <div className="flex flex-col items-stretch justify-between gap-3 lg:flex-row lg:items-end">
        <div className="min-w-0 space-y-2">
          <div className="inline-flex items-center gap-2 rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
            <Database className="size-3.5" />
            Settings · Data Source
          </div>
          <h1 className="text-2xl font-semibold tracking-tight text-foreground sm:text-4xl">
            数据源有效日期监控
          </h1>
          <p className="max-w-3xl text-sm leading-7 text-muted-foreground sm:text-base">
            按 PostgreSQL 与 ClickHouse 表维度汇总最新业务日期、行数和滞后状态，并补充表的业务逻辑、使用位置、更新 Job 与数据源。
          </p>
        </div>
        <div className="flex flex-col items-stretch gap-1.5 lg:items-end">
          <Button
            size="sm"
            variant="secondary"
            className="rounded-xl"
            onClick={() => void refresh(true)}
            disabled={loading}
          >
            <RefreshCcw className="size-3.5" />
            reload
          </Button>
          <div className="text-xs text-muted-foreground">
            {data?.generatedAt
              ? `最近刷新：${formatDateTime(data.generatedAt)} · 每 ${REFRESH_INTERVAL_MS / 1000}s 自动刷新`
              : `每 ${REFRESH_INTERVAL_MS / 1000}s 自动刷新`}
          </div>
        </div>
      </div>

      {data?.summary ? <FreshnessSummary summary={data.summary} /> : null}

      <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div className="relative w-full xl:max-w-sm">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="搜索表名、schema、业务、Job 或数据源"
            className="pl-9"
          />
        </div>
        <div className="flex max-w-full flex-col gap-2 overflow-x-auto sm:flex-row sm:items-center">
          <Tabs value={sourceFilter} onValueChange={(value) => setSourceFilter(value as SourceFilter)}>
            <TabsList className="min-w-max">
              <TabsTrigger value="all">All</TabsTrigger>
              <TabsTrigger value="postgres">PostgreSQL</TabsTrigger>
              <TabsTrigger value="clickhouse">ClickHouse</TabsTrigger>
            </TabsList>
          </Tabs>
          <Tabs value={statusFilter} onValueChange={(value) => setStatusFilter(value as StatusFilter)}>
            <TabsList className="min-w-max">
              <TabsTrigger value="all">全部 {statusCounts.all}</TabsTrigger>
              <TabsTrigger value="fresh">正常 {statusCounts.fresh}</TabsTrigger>
              <TabsTrigger value="lagging">滞后 {statusCounts.lagging}</TabsTrigger>
              <TabsTrigger value="stale">过期 {statusCounts.stale}</TabsTrigger>
              <TabsTrigger value="error">异常 {statusCounts.error}</TabsTrigger>
              <TabsTrigger value="untracked">未识别 {statusCounts.untracked}</TabsTrigger>
            </TabsList>
          </Tabs>
        </div>
      </div>

      {error ? (
        <Alert variant="destructive">
          <AlertTriangle className="size-4" />
          <AlertTitle>拉取数据源状态失败</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}

      {loading && !data ? (
        <DogLoader overlay size={25} label="正在加载数据源状态..." />
      ) : (
        <FreshnessTable items={items as DataSourceFreshnessItem[]} />
      )}
    </WorkspaceShell>
  )
}
