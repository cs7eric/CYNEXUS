import { useCallback, useEffect, useMemo, useState } from "react"
import { Check, ChevronDown, Database, RefreshCw, Search, Server, ShieldCheck, X } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { cn } from "@/lib/utils"
import {
  fetchProviderCapabilities,
  type ProviderCapabilityItem,
  type ProviderCapabilityResponse,
} from "@/services/core/provider-capabilities"

function label(value?: string | null) {
  return value && value.trim() ? value : "-"
}

function join(value?: string[] | null) {
  return value?.length ? value.join(" / ") : "-"
}

function CompactBadge({
  value,
  variant = "secondary",
  className,
}: {
  value?: string | null
  variant?: "secondary" | "outline"
  className?: string
}) {
  const text = label(value)
  return (
    <Badge variant={variant} className={cn("max-w-full justify-start overflow-hidden px-2", className)} title={text}>
      <span className="min-w-0 truncate">{text}</span>
    </Badge>
  )
}

function categoryLabel(value: string) {
  const labels: Record<string, string> = {
    storage: "存储",
    metadata: "元数据",
    kline: "K线",
    intraday: "盘中",
    "a-stock-skill": "A股 Skill",
    sector: "行业/板块",
    "market-app": "市场应用",
    msi: "情绪指数",
    margin: "两融",
    "fund-flow": "资金流",
    media: "媒体",
    ops: "运维",
  }
  return labels[value] || value
}

function HeaderFilterMultiSelect({
  title,
  selected,
  options,
  onChange,
  format = (item) => item,
}: {
  title: string
  selected: string[]
  options: string[]
  onChange: (value: string[]) => void
  format?: (item: string) => string
}) {
  const selectedSet = useMemo(() => new Set(selected), [selected])
  const active = selected.length > 0
  const text = !active ? title : selected.length === 1 ? format(selected[0]) : `${title} · ${selected.length}`

  const toggle = (item: string) => {
    onChange(selectedSet.has(item) ? selected.filter((value) => value !== item) : [...selected, item])
  }

  return (
    <div className="flex min-w-0 items-center gap-1">
      <Popover>
        <PopoverTrigger asChild>
          <button
            type="button"
            className={cn(
              "flex h-8 min-w-0 flex-1 items-center justify-start gap-1 bg-transparent px-0 text-xs font-semibold outline-none",
              active ? "text-foreground" : "text-muted-foreground"
            )}
            title={text}
          >
            <span className="min-w-0 truncate">{text}</span>
            <ChevronDown className="size-3.5 shrink-0 opacity-60" />
          </button>
        </PopoverTrigger>
        <PopoverContent
          align="start"
          className="max-h-64 w-64 overflow-y-auto border-0 bg-muted p-1 shadow-lg [scrollbar-color:rgba(120,120,120,0.35)_transparent] [scrollbar-width:thin] [&::-webkit-scrollbar]:w-1 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-neutral-300"
        >
          {options.map((item) => (
            <div
              key={item}
              role="option"
              aria-selected={selectedSet.has(item)}
              tabIndex={0}
              className={cn(
                "relative cursor-default rounded-sm px-2 py-1.5 text-xs outline-none hover:bg-white focus:bg-white",
                selectedSet.has(item) && "bg-white"
              )}
              onClick={() => toggle(item)}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault()
                  toggle(item)
                }
              }}
            >
              <span className="block min-w-0 truncate pr-5" title={format(item)}>{format(item)}</span>
              {selectedSet.has(item) ? (
                <Check className="absolute right-2 top-1/2 size-3.5 -translate-y-1/2 text-foreground" />
              ) : null}
            </div>
          ))}
        </PopoverContent>
      </Popover>
      {active ? (
        <Button
          type="button"
          variant="ghost"
          size="icon-xs"
          className="shrink-0 text-muted-foreground hover:bg-white hover:text-foreground"
          onClick={() => onChange([])}
          aria-label={`Clear ${title} filter`}
        >
          <X className="size-3" />
        </Button>
      ) : null}
    </div>
  )
}

function SummaryStrip({ data }: { data: ProviderCapabilityResponse }) {
  const stats = [
    { label: "能力", value: data.summary.total, icon: ShieldCheck },
    { label: "Provider", value: Object.keys(data.summary.byProvider || {}).length, icon: Server },
    { label: "分类", value: Object.keys(data.summary.byCategory || {}).length, icon: Database },
  ]
  return (
    <div className="grid gap-3 sm:grid-cols-3">
      {stats.map((item) => {
        const Icon = item.icon
        return (
          <div key={item.label} className="rounded-md bg-muted/60 px-4 py-3">
            <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
              <Icon className="size-4" />
              {item.label}
            </div>
            <div className="mt-2 font-mono text-2xl font-semibold text-foreground">{item.value}</div>
          </div>
        )
      })}
    </div>
  )
}

function CapabilityTable({
  items,
  providers,
  categories,
  capabilityTypes,
  granularities,
  stores,
  selectedProviders,
  selectedCategories,
  selectedCapabilityTypes,
  selectedGranularities,
  selectedTargetStores,
  setSelectedProviders,
  setSelectedCategories,
  setSelectedCapabilityTypes,
  setSelectedGranularities,
  setSelectedTargetStores,
}: {
  items: ProviderCapabilityItem[]
  providers: string[]
  categories: string[]
  capabilityTypes: string[]
  granularities: string[]
  stores: string[]
  selectedProviders: string[]
  selectedCategories: string[]
  selectedCapabilityTypes: string[]
  selectedGranularities: string[]
  selectedTargetStores: string[]
  setSelectedProviders: (value: string[]) => void
  setSelectedCategories: (value: string[]) => void
  setSelectedCapabilityTypes: (value: string[]) => void
  setSelectedGranularities: (value: string[]) => void
  setSelectedTargetStores: (value: string[]) => void
}) {
  return (
    <div className="overflow-hidden rounded-md bg-muted/50 [&_[data-slot=table-container]]:max-h-[66vh] [&_[data-slot=table-container]]:overflow-auto">
      <Table className="min-w-[2140px] table-fixed">
        <TableHeader className="[&_th]:sticky [&_th]:top-0 [&_th]:z-20 [&_th]:bg-muted">
          <TableRow>
            <TableHead className="w-[280px]">Capability</TableHead>
            <TableHead className="w-[220px]">
              <HeaderFilterMultiSelect
                title="Provider"
                selected={selectedProviders}
                options={providers}
                onChange={setSelectedProviders}
              />
            </TableHead>
            <TableHead className="w-[160px]">
              <HeaderFilterMultiSelect
                title="Category"
                selected={selectedCategories}
                options={categories}
                onChange={setSelectedCategories}
                format={categoryLabel}
              />
            </TableHead>
            <TableHead className="w-[160px]">
              <HeaderFilterMultiSelect
                title="Type"
                selected={selectedCapabilityTypes}
                options={capabilityTypes}
                onChange={setSelectedCapabilityTypes}
              />
            </TableHead>
            <TableHead className="w-[170px]">
              <HeaderFilterMultiSelect
                title="Granularity"
                selected={selectedGranularities}
                options={granularities}
                onChange={setSelectedGranularities}
              />
            </TableHead>
            <TableHead className="w-[150px]">
              <HeaderFilterMultiSelect
                title="Target"
                selected={selectedTargetStores}
                options={stores}
                onChange={setSelectedTargetStores}
              />
            </TableHead>
            <TableHead className="w-[300px]">Tables</TableHead>
            <TableHead className="w-[260px]">Jobs</TableHead>
            <TableHead className="w-[300px]">Used By</TableHead>
            <TableHead className="w-[330px]">Policy</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.map((item) => (
            <TableRow key={item.capabilityCode}>
              <TableCell className="whitespace-normal">
                <div className="max-w-[260px]">
                  <div className="truncate font-medium text-foreground" title={item.name}>{item.name}</div>
                  <div className="truncate font-mono text-xs text-muted-foreground">{item.capabilityCode}</div>
                  <div className="mt-1 line-clamp-2 text-xs leading-5 text-muted-foreground" title={item.description || undefined}>
                    {label(item.description)}
                  </div>
                </div>
              </TableCell>
              <TableCell className="overflow-hidden">
                <div className="min-w-0">
                  <CompactBadge value={item.providerCode} className="uppercase" />
                  <div className="mt-1 truncate text-xs text-muted-foreground" title={item.providerName}>
                    {item.providerName}
                  </div>
                </div>
              </TableCell>
              <TableCell className="overflow-hidden">
                <div className="flex min-w-0 flex-col gap-1">
                  <span className="truncate text-sm font-medium" title={categoryLabel(item.category)}>
                    {categoryLabel(item.category)}
                  </span>
                </div>
              </TableCell>
              <TableCell className="overflow-hidden text-xs text-muted-foreground">
                <div className="truncate" title={item.capabilityType}>{item.capabilityType}</div>
              </TableCell>
              <TableCell className="font-mono text-xs text-muted-foreground">{label(item.dataGranularity)}</TableCell>
              <TableCell className="overflow-hidden">
                <CompactBadge value={item.targetStore} variant="outline" className="uppercase" />
              </TableCell>
              <TableCell className="whitespace-normal">
                <div className="line-clamp-3 max-w-[280px] font-mono text-xs leading-5 text-muted-foreground" title={join(item.storageTables)}>
                  {join(item.storageTables)}
                </div>
              </TableCell>
              <TableCell className="whitespace-normal">
                <div className="line-clamp-3 max-w-[240px] font-mono text-xs leading-5 text-foreground" title={join(item.updateJobs)}>
                  {join(item.updateJobs)}
                </div>
              </TableCell>
              <TableCell className="whitespace-normal">
                <div className="line-clamp-3 max-w-[280px] text-xs leading-5 text-muted-foreground" title={join(item.usedBy)}>
                  {join(item.usedBy)}
                </div>
              </TableCell>
              <TableCell className="whitespace-normal">
                <div className="max-w-[310px] space-y-1 text-xs leading-5">
                  <div className="line-clamp-1" title={item.freshnessPolicy || undefined}>新鲜度：{label(item.freshnessPolicy)}</div>
                  <div className="line-clamp-1 text-muted-foreground" title={item.rateLimitPolicy || undefined}>频率：{label(item.rateLimitPolicy)}</div>
                  <div className="line-clamp-1 text-muted-foreground" title={item.antiScrapingNotes || undefined}>防爬：{label(item.antiScrapingNotes)}</div>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

export default function ProviderSettingsPage() {
  const [data, setData] = useState<ProviderCapabilityResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [query, setQuery] = useState("")
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])
  const [selectedProviders, setSelectedProviders] = useState<string[]>([])
  const [selectedCapabilityTypes, setSelectedCapabilityTypes] = useState<string[]>([])
  const [selectedGranularities, setSelectedGranularities] = useState<string[]>([])
  const [selectedTargetStores, setSelectedTargetStores] = useState<string[]>([])

  const reload = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      setData(await fetchProviderCapabilities())
    } catch (err) {
      setError(err instanceof Error ? err.message : "Provider 能力目录加载失败")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const categories = useMemo(() => [...new Set((data?.items || []).map((item) => item.category))].sort(), [data?.items])
  const providers = useMemo(() => [...new Set((data?.items || []).map((item) => item.providerCode))].sort(), [data?.items])
  const capabilityTypes = useMemo(() => [...new Set((data?.items || []).map((item) => item.capabilityType).filter(Boolean))].sort(), [data?.items])
  const granularities = useMemo(() => [...new Set((data?.items || []).map((item) => item.dataGranularity || "").filter(Boolean))].sort(), [data?.items])
  const stores = useMemo(() => [...new Set((data?.items || []).map((item) => item.targetStore))].sort(), [data?.items])

  const items = useMemo(() => {
    const q = query.trim().toLowerCase()
    return (data?.items || []).filter((item) => {
      const matchesCategory = selectedCategories.length === 0 || selectedCategories.includes(item.category)
      const matchesProvider = selectedProviders.length === 0 || selectedProviders.includes(item.providerCode)
      const matchesCapabilityType = selectedCapabilityTypes.length === 0 || selectedCapabilityTypes.includes(item.capabilityType)
      const matchesGranularity = selectedGranularities.length === 0 || selectedGranularities.includes(item.dataGranularity || "")
      const matchesStore = selectedTargetStores.length === 0 || selectedTargetStores.includes(item.targetStore)
      const haystack = [
        item.capabilityCode,
        item.name,
        item.providerCode,
        item.providerName,
        item.category,
        item.capabilityType,
        item.description,
        item.freshnessPolicy,
        item.rateLimitPolicy,
        item.antiScrapingNotes,
        ...item.storageTables,
        ...item.updateJobs,
        ...item.usedBy,
      ].join(" ").toLowerCase()
      return matchesCategory && matchesProvider && matchesCapabilityType && matchesGranularity && matchesStore && (!q || haystack.includes(q))
    })
  }, [data?.items, query, selectedCapabilityTypes, selectedCategories, selectedGranularities, selectedProviders, selectedTargetStores])

  return (
    <WorkspaceShell>
      <div className="flex w-full flex-col gap-5">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <Server className="size-4" />
              Settings
            </div>
            <h1 className="text-2xl font-semibold tracking-tight text-foreground">Provider</h1>
            <p className="max-w-3xl text-sm leading-6 text-muted-foreground">
              cynexus-data 已接入能力目录，覆盖行情、K 线、资金流、行业、研报、资讯、期权、两融、MSI 和运维健康检查。
            </p>
          </div>
          <Button variant="outline" onClick={() => void reload()} disabled={loading}>
            <RefreshCw className={cn("size-4", loading && "animate-spin")} />
            Refresh
          </Button>
        </div>

        {error ? (
          <Alert variant="destructive">
            <AlertTitle>Provider 能力目录加载失败</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        ) : null}

        {data ? <SummaryStrip data={data} /> : null}

        <div className="max-w-2xl">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="pl-9"
              placeholder="搜索能力、Provider、表、Job、用途"
            />
          </div>
        </div>

        {data ? (
          <div className="space-y-2">
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>共 {items.length} / {data.summary.total} 项</span>
              <span>更新于 {new Date(data.generatedAt).toLocaleString()}</span>
            </div>
            <CapabilityTable
              items={items}
              providers={providers}
              categories={categories}
              capabilityTypes={capabilityTypes}
              granularities={granularities}
              stores={stores}
              selectedProviders={selectedProviders}
              selectedCategories={selectedCategories}
              selectedCapabilityTypes={selectedCapabilityTypes}
              selectedGranularities={selectedGranularities}
              selectedTargetStores={selectedTargetStores}
              setSelectedProviders={setSelectedProviders}
              setSelectedCategories={setSelectedCategories}
              setSelectedCapabilityTypes={setSelectedCapabilityTypes}
              setSelectedGranularities={setSelectedGranularities}
              setSelectedTargetStores={setSelectedTargetStores}
            />
          </div>
        ) : (
          <div className="rounded-md bg-muted/60 p-8 text-sm text-muted-foreground">加载中...</div>
        )}
      </div>
    </WorkspaceShell>
  )
}
