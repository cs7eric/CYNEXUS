import { useCallback, useEffect, useMemo, useState } from "react"
import { Activity, AlertTriangle, Database, RefreshCcw, Search, ShieldCheck } from "lucide-react"

import DogLoader from "@/components/loader/dog-loader"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { WorkspaceShell } from "@/layout/workspace-shell"
import {
  type DataSourceHealthItem,
  type DataSourceHealthResponse,
  fetchDataSourceHealthLatest,
  runDataSourceHealthCheck,
} from "@/services/core/data-source-health"
import {
  type DataSourceFreshnessItem,
  type DataSourceFreshnessResponse,
  type DataSourceStatus,
  fetchDataSourceFreshness,
} from "@/services/market/data-source"
import { FreshnessSummary } from "./components/freshness-summary"
import { FreshnessTable } from "./components/freshness-table"
import { HealthSummary } from "./components/health-summary"
import { HealthTable } from "./components/health-table"
import { formatDateTime } from "./lib/format"

type ViewMode = "health" | "freshness"
type SourceFilter = "all" | "postgres" | "clickhouse" | "http" | "tcp" | "python" | "filesystem" | "duckdb"
type FreshnessStatusFilter = "all" | DataSourceStatus
type HealthStatusFilter = "all" | "healthy" | "degraded" | "failed" | "skipped"

const REFRESH_INTERVAL_MS = 30_000

export default function DataSourceSettingsPage() {
  const [viewMode, setViewMode] = useState<ViewMode>("health")
  const [freshnessData, setFreshnessData] = useState<DataSourceFreshnessResponse | null>(null)
  const [healthData, setHealthData] = useState<DataSourceHealthResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [healthRunning, setHealthRunning] = useState(false)
  const [healthError, setHealthError] = useState<string | null>(null)
  const [freshnessError, setFreshnessError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>("all")
  const [freshnessStatusFilter, setFreshnessStatusFilter] = useState<FreshnessStatusFilter>("all")
  const [healthStatusFilter, setHealthStatusFilter] = useState<HealthStatusFilter>("all")

  const refresh = useCallback(async (showLoading = true) => {
    if (showLoading) setLoading(true)
    const [health, freshness] = await Promise.allSettled([
      fetchDataSourceHealthLatest(),
      fetchDataSourceFreshness(),
    ])
    if (health.status === "fulfilled") {
      setHealthData(health.value)
      setHealthError(null)
    } else {
      setHealthError(health.reason instanceof Error ? health.reason.message : "数据源健康检查拉取失败")
    }
    if (freshness.status === "fulfilled") {
      setFreshnessData(freshness.value)
      setFreshnessError(null)
    } else {
      setFreshnessError(freshness.reason instanceof Error ? freshness.reason.message : "数据表新鲜度拉取失败")
    }
    if (showLoading) setLoading(false)
  }, [])

  const runHealth = useCallback(async (includeSlow: boolean) => {
    setHealthRunning(true)
    setHealthError(null)
    try {
      setHealthData(await runDataSourceHealthCheck(includeSlow))
      setViewMode("health")
    } catch (err) {
      setHealthError(err instanceof Error ? err.message : "数据源健康检查失败")
    } finally {
      setHealthRunning(false)
    }
  }, [])

  useEffect(() => {
    void refresh(true)
    const timer = window.setInterval(() => void refresh(false), REFRESH_INTERVAL_MS)
    return () => {
      window.clearInterval(timer)
    }
  }, [refresh])

  const healthItems = useMemo(() => {
    const keyword = query.trim().toLowerCase()
    return (healthData?.items || []).filter((item) => {
      const matchesSource = sourceFilter === "all" || item.sourceType === sourceFilter || item.category === sourceFilter
      const matchesStatus = healthStatusFilter === "all" || item.status === healthStatusFilter
      const haystack = [
        item.sourceCode,
        item.sourceName,
        item.sourceType,
        item.category,
        item.provider,
        item.endpoint,
        item.status,
        item.authenticityStatus,
        item.errorMessage,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
      return matchesSource && matchesStatus && (!keyword || haystack.includes(keyword))
    })
  }, [healthData?.items, healthStatusFilter, query, sourceFilter])

  const freshnessItems = useMemo(() => {
    const keyword = query.trim().toLowerCase()
    return (freshnessData?.items || []).filter((item) => {
      const matchesSource = sourceFilter === "all" || item.sourceType === sourceFilter
      const matchesStatus = freshnessStatusFilter === "all" || item.status === freshnessStatusFilter
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
  }, [freshnessData?.items, freshnessStatusFilter, query, sourceFilter])

  const healthCounts = useMemo(() => ({
    all: healthData?.summary.total || 0,
    healthy: healthData?.summary.healthy || 0,
    degraded: healthData?.summary.degraded || 0,
    failed: healthData?.summary.failed || 0,
    skipped: healthData?.summary.skipped || 0,
  }), [healthData?.summary])

  const freshnessCounts = useMemo(() => ({
    all: freshnessData?.summary.total || 0,
    fresh: freshnessData?.summary.fresh || 0,
    lagging: freshnessData?.summary.lagging || 0,
    stale: freshnessData?.summary.stale || 0,
    empty: freshnessData?.summary.empty || 0,
    untracked: freshnessData?.summary.untracked || 0,
    error: freshnessData?.summary.error || 0,
  }), [freshnessData?.summary])

  const generatedAt = viewMode === "health" ? healthData?.generatedAt : freshnessData?.generatedAt
  const activeError = viewMode === "health" ? healthError : freshnessError

  return (
    <WorkspaceShell sectionLabel="Settings" pageTitle="Data Source">
      <div className="flex flex-col items-stretch justify-between gap-3 lg:flex-row lg:items-end">
        <div className="min-w-0 space-y-2">
          <div className="inline-flex items-center gap-2 rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
            {viewMode === "health" ? <ShieldCheck className="size-3.5" /> : <Database className="size-3.5" />}
            Settings · Data Source
          </div>
          <h1 className="text-2xl font-semibold tracking-tight text-foreground sm:text-4xl">
            数据源健康检查
          </h1>
          <p className="max-w-3xl text-sm leading-7 text-muted-foreground sm:text-base">
            从 Python 执行器检查 provider 连通性、样本真实性、响应耗时和历史成功率；同时保留 PostgreSQL 与 ClickHouse 表有效日期监控。
          </p>
        </div>
        <div className="flex flex-col items-stretch gap-1.5 lg:items-end">
          <div className="flex flex-wrap justify-end gap-2">
            <Button
              size="sm"
              variant="secondary"
              className="rounded-xl"
              onClick={() => void refresh(true)}
              disabled={loading || healthRunning}
            >
              <RefreshCcw className="size-3.5" />
              reload
            </Button>
            <Button
              size="sm"
              className="rounded-xl"
              onClick={() => void runHealth(false)}
              disabled={loading || healthRunning}
            >
              <Activity className={healthRunning ? "size-3.5 animate-spin" : "size-3.5"} />
              quick check
            </Button>
            <Button
              size="sm"
              variant="outline"
              className="rounded-xl"
              onClick={() => void runHealth(true)}
              disabled={loading || healthRunning}
            >
              <ShieldCheck className="size-3.5" />
              full check
            </Button>
          </div>
          <div className="text-xs text-muted-foreground">
            {generatedAt
              ? `最近刷新：${formatDateTime(generatedAt)} · 每 ${REFRESH_INTERVAL_MS / 1000}s 自动刷新`
              : `每 ${REFRESH_INTERVAL_MS / 1000}s 自动刷新`}
          </div>
        </div>
      </div>

      <Tabs value={viewMode} onValueChange={(value) => setViewMode(value as ViewMode)}>
        <TabsList className="w-fit">
          <TabsTrigger value="health">Source Health</TabsTrigger>
          <TabsTrigger value="freshness">Table Freshness</TabsTrigger>
        </TabsList>
      </Tabs>

      {viewMode === "health" && healthData?.summary ? <HealthSummary summary={healthData.summary} /> : null}
      {viewMode === "freshness" && freshnessData?.summary ? <FreshnessSummary summary={freshnessData.summary} /> : null}

      <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div className="relative w-full xl:max-w-sm">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={viewMode === "health" ? "搜索 provider、endpoint、状态或错误" : "搜索表名、schema、业务、Job 或数据源"}
            className="pl-9"
          />
        </div>
        <div className="flex max-w-full flex-col gap-2 overflow-x-auto sm:flex-row sm:items-center">
          <Tabs value={sourceFilter} onValueChange={(value) => setSourceFilter(value as SourceFilter)}>
            <TabsList className="min-w-max">
              <TabsTrigger value="all">All</TabsTrigger>
              <TabsTrigger value="postgres">PostgreSQL</TabsTrigger>
              <TabsTrigger value="clickhouse">ClickHouse</TabsTrigger>
              {viewMode === "health" ? (
                <>
                  <TabsTrigger value="http">HTTP</TabsTrigger>
                  <TabsTrigger value="tcp">TCP</TabsTrigger>
                  <TabsTrigger value="python">Python</TabsTrigger>
                  <TabsTrigger value="filesystem">File</TabsTrigger>
                </>
              ) : null}
            </TabsList>
          </Tabs>
          {viewMode === "health" ? (
            <Tabs value={healthStatusFilter} onValueChange={(value) => setHealthStatusFilter(value as HealthStatusFilter)}>
              <TabsList className="min-w-max">
                <TabsTrigger value="all">全部 {healthCounts.all}</TabsTrigger>
                <TabsTrigger value="healthy">健康 {healthCounts.healthy}</TabsTrigger>
                <TabsTrigger value="degraded">降级 {healthCounts.degraded}</TabsTrigger>
                <TabsTrigger value="failed">失败 {healthCounts.failed}</TabsTrigger>
                <TabsTrigger value="skipped">跳过 {healthCounts.skipped}</TabsTrigger>
              </TabsList>
            </Tabs>
          ) : (
            <Tabs value={freshnessStatusFilter} onValueChange={(value) => setFreshnessStatusFilter(value as FreshnessStatusFilter)}>
              <TabsList className="min-w-max">
                <TabsTrigger value="all">全部 {freshnessCounts.all}</TabsTrigger>
                <TabsTrigger value="fresh">正常 {freshnessCounts.fresh}</TabsTrigger>
                <TabsTrigger value="lagging">滞后 {freshnessCounts.lagging}</TabsTrigger>
                <TabsTrigger value="stale">过期 {freshnessCounts.stale}</TabsTrigger>
                <TabsTrigger value="error">异常 {freshnessCounts.error}</TabsTrigger>
                <TabsTrigger value="untracked">未识别 {freshnessCounts.untracked}</TabsTrigger>
              </TabsList>
            </Tabs>
          )}
        </div>
      </div>

      {activeError ? (
        <Alert variant="destructive">
          <AlertTriangle className="size-4" />
          <AlertTitle>拉取数据源状态失败</AlertTitle>
          <AlertDescription>{activeError}</AlertDescription>
        </Alert>
      ) : null}

      {loading && !healthData && !freshnessData ? (
        <DogLoader overlay size={25} label="正在加载数据源状态..." />
      ) : viewMode === "health" ? (
        <HealthTable items={healthItems as DataSourceHealthItem[]} />
      ) : (
        <FreshnessTable items={freshnessItems as DataSourceFreshnessItem[]} />
      )}
    </WorkspaceShell>
  )
}
