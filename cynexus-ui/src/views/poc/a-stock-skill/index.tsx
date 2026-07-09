import { useCallback, useEffect, useMemo, useState } from "react"
import { CheckCircle2, Database, RefreshCw, Search } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { cn } from "@/lib/utils"
import {
  fetchAStockSkillDatasets,
  fetchAStockSkillRows,
  type AStockSkillDataset,
  type AStockSkillRows,
} from "@/services/market/a-stock-skill"

const DEFAULT_DATASET = "limit_pool"

function compact(value: unknown): string {
  if (value === null || value === undefined || value === "") return "-"
  if (typeof value === "object") return JSON.stringify(value)
  return String(value)
}

function formatCount(value?: number | null): string {
  if (value === null || value === undefined) return "-"
  return new Intl.NumberFormat("zh-CN").format(value)
}

function formatTime(value?: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").replace(/\.\d+.*$/, "")
}

function DatasetItem({
  dataset,
  active,
  onClick,
}: {
  dataset: AStockSkillDataset
  active: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "w-full rounded-md px-3 py-2 text-left transition-colors hover:bg-white",
        active && "bg-white shadow-sm",
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="truncate text-sm font-medium text-foreground">{dataset.name}</div>
          <div className="mt-1 truncate font-mono text-[11px] text-muted-foreground">
            {dataset.tableName}
          </div>
        </div>
        {active ? <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-emerald-600" /> : null}
      </div>
      <div className="mt-2 flex items-center gap-2">
        <Badge variant="outline" className="px-1.5 py-0 text-[10px] uppercase">
          {dataset.store}
        </Badge>
        <span className="truncate text-xs text-muted-foreground">{formatCount(dataset.rowCount)} rows</span>
      </div>
    </button>
  )
}

export default function AStockSkillPoc() {
  const [datasets, setDatasets] = useState<AStockSkillDataset[]>([])
  const [datasetCode, setDatasetCode] = useState(DEFAULT_DATASET)
  const [rowsData, setRowsData] = useState<AStockSkillRows | null>(null)
  const [query, setQuery] = useState("")
  const [code, setCode] = useState("")
  const [source, setSource] = useState("")
  const [limit, setLimit] = useState("50")
  const [loadingDatasets, setLoadingDatasets] = useState(true)
  const [loadingRows, setLoadingRows] = useState(false)
  const [error, setError] = useState("")

  const activeDataset = useMemo(
    () => datasets.find((item) => item.code === datasetCode) || datasets[0] || null,
    [datasets, datasetCode],
  )

  const grouped = useMemo(() => {
    const q = query.trim().toLowerCase()
    const filtered = q
      ? datasets.filter((item) =>
          [item.code, item.name, item.groupName, item.description, item.store, item.tableName, item.grain]
            .join(" ")
            .toLowerCase()
            .includes(q),
        )
      : datasets
    return filtered.reduce<Record<string, AStockSkillDataset[]>>((acc, item) => {
      const key = item.groupName || "其他"
      acc[key] = acc[key] || []
      acc[key].push(item)
      return acc
    }, {})
  }, [datasets, query])

  const loadRows = useCallback(
    async (nextDatasetCode = datasetCode) => {
      setLoadingRows(true)
      setError("")
      try {
        const data = await fetchAStockSkillRows({
          datasetCode: nextDatasetCode,
          limit: Number(limit) || 50,
          code,
          source,
        })
        setRowsData(data)
      } catch (err) {
        setError(err instanceof Error ? err.message : "A股 Skill 数据加载失败")
      } finally {
        setLoadingRows(false)
      }
    },
    [code, datasetCode, limit, source],
  )

  const loadDatasets = useCallback(async () => {
    setLoadingDatasets(true)
    setError("")
    try {
      const data = await fetchAStockSkillDatasets()
      const nextDatasets = data.datasets || []
      setDatasets(nextDatasets)
      const nextCode = nextDatasets.some((item) => item.code === datasetCode)
        ? datasetCode
        : nextDatasets[0]?.code || DEFAULT_DATASET
      setDatasetCode(nextCode)
      if (nextCode) {
        await loadRows(nextCode)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "A股 Skill 数据集加载失败")
    } finally {
      setLoadingDatasets(false)
    }
  }, [datasetCode, loadRows])

  useEffect(() => {
    void loadDatasets()
  }, [])

  function selectDataset(nextCode: string) {
    setDatasetCode(nextCode)
    void loadRows(nextCode)
  }

  const columns = rowsData?.columns || activeDataset?.columns || []
  const rows = rowsData?.rows || []

  return (
    <div className="flex min-h-[calc(100vh-8rem)] flex-col gap-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 className="text-xl font-semibold tracking-tight text-foreground">A股 Skill 实际数据</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            这里直接读取已落库的 PostgreSQL / ClickHouse A股 Skill 数据表。
          </p>
        </div>
        <Button variant="outline" onClick={() => void loadRows()} disabled={loadingRows || !activeDataset}>
          <RefreshCw className={cn("size-4", loadingRows && "animate-spin")} />
          Refresh
        </Button>
      </div>

      {error ? (
        <Alert variant="destructive">
          <AlertTitle>数据读取失败</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : null}

      <div className="grid min-h-0 flex-1 gap-4 xl:grid-cols-[340px_minmax(0,1fr)]">
        <aside className="min-h-0 rounded-md bg-muted/50 p-3">
          <div className="relative mb-3">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="bg-white pl-9"
              placeholder="搜索数据源能力"
            />
          </div>
          <div className="max-h-[68vh] space-y-4 overflow-auto pr-1">
            {loadingDatasets ? (
              <div className="py-8 text-center text-sm text-muted-foreground">Loading...</div>
            ) : (
              Object.entries(grouped).map(([groupName, items]) => (
                <section key={groupName} className="space-y-2">
                  <div className="px-1 text-xs font-medium text-muted-foreground">{groupName}</div>
                  <div className="space-y-1">
                    {items.map((item) => (
                      <DatasetItem
                        key={item.code}
                        dataset={item}
                        active={item.code === activeDataset?.code}
                        onClick={() => selectDataset(item.code)}
                      />
                    ))}
                  </div>
                </section>
              ))
            )}
          </div>
        </aside>

        <main className="min-w-0 space-y-3">
          <div className="rounded-md bg-muted/50 p-3">
            <div className="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <Database className="size-4 text-muted-foreground" />
                  <h3 className="truncate text-base font-semibold text-foreground">
                    {activeDataset?.name || "-"}
                  </h3>
                  {activeDataset ? (
                    <Badge variant="secondary" className="uppercase">
                      {activeDataset.store}
                    </Badge>
                  ) : null}
                </div>
                <div className="mt-1 line-clamp-2 text-sm text-muted-foreground">
                  {activeDataset?.description || "-"}
                </div>
                <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 font-mono text-xs text-muted-foreground">
                  <span>{activeDataset?.schemaName}.{activeDataset?.tableName}</span>
                  <span>grain: {activeDataset?.grain || "-"}</span>
                  <span>latest: {formatTime(activeDataset?.latestAt)}</span>
                  <span>rows: {formatCount(activeDataset?.rowCount)}</span>
                </div>
              </div>
              <div className="grid gap-2 sm:grid-cols-[160px_140px_140px_110px]">
                <Input value={code} onChange={(event) => setCode(event.target.value)} placeholder="code / option_code" />
                <Input value={source} onChange={(event) => setSource(event.target.value)} placeholder="source" />
                <Select value={limit} onValueChange={setLimit}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="20">20 rows</SelectItem>
                    <SelectItem value="50">50 rows</SelectItem>
                    <SelectItem value="100">100 rows</SelectItem>
                    <SelectItem value="200">200 rows</SelectItem>
                  </SelectContent>
                </Select>
                <Button onClick={() => void loadRows()} disabled={loadingRows || !activeDataset}>
                  查询
                </Button>
              </div>
            </div>
          </div>

          <div className="overflow-hidden rounded-md bg-muted/50">
            <div className="max-h-[64vh] overflow-auto">
              <Table className="min-w-[1200px]">
                <TableHeader className="[&_th]:sticky [&_th]:top-0 [&_th]:z-20 [&_th]:bg-muted">
                  <TableRow>
                    {columns.map((column) => (
                      <TableHead key={column} className="whitespace-nowrap font-mono text-xs">
                        {column}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rows.length ? (
                    rows.map((row, index) => (
                      <TableRow key={index}>
                        {columns.map((column) => {
                          const value = compact(row[column])
                          return (
                            <TableCell key={column} className="max-w-[320px] align-top">
                              <div className="line-clamp-3 break-words font-mono text-xs leading-5" title={value}>
                                {value}
                              </div>
                            </TableCell>
                          )
                        })}
                      </TableRow>
                    ))
                  ) : (
                    <TableRow>
                      <TableCell colSpan={Math.max(columns.length, 1)} className="h-32 text-center text-muted-foreground">
                        {loadingRows ? "Loading..." : "暂无数据"}
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
