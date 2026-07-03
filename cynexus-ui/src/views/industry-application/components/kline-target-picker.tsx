import { useMemo, useState } from "react"
import { Building2, Check, ChevronsUpDown, LineChart, Sparkles } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { cn } from "@/lib/utils"

import type {
  IndustryApplicationTargetCode,
  IndustryApplicationTargetKind,
} from "../lib/types"

export interface KlineTargetPickerValue {
  code: string
  name: string
  target_type: IndustryApplicationTargetKind
}

interface KlineTargetPickerProps {
  items: IndustryApplicationTargetCode[]
  value: KlineTargetPickerValue | null
  onSelect: (item: IndustryApplicationTargetCode) => void
  className?: string
}

const KIND_META: Record<
  IndustryApplicationTargetKind,
  { label: string; commandGroup: string; badgeClassName: string; icon: typeof Building2 }
> = {
  industry: {
    label: "行业",
    commandGroup: "行业板块",
    badgeClassName: "border-blue-200 bg-blue-50 text-blue-700",
    icon: Building2,
  },
  concept: {
    label: "概念",
    commandGroup: "概念板块",
    badgeClassName: "border-violet-200 bg-violet-50 text-violet-700",
    icon: Sparkles,
  },
  index: {
    label: "指数",
    commandGroup: "宽基指数",
    badgeClassName: "border-emerald-200 bg-emerald-50 text-emerald-700",
    icon: LineChart,
  },
}

export function KlineTargetPicker({
  items,
  value,
  onSelect,
  className,
}: KlineTargetPickerProps) {
  const [open, setOpen] = useState(false)
  const [kindFilter, setKindFilter] = useState<IndustryApplicationTargetKind | "all">("all")

  const groupedItems = useMemo(
    () => ({
      index: items.filter((item) => item.kind === "index"),
      industry: items.filter((item) => item.kind === "industry"),
      concept: items.filter((item) => item.kind === "concept"),
    }),
    [items],
  )

  const visibleKinds = useMemo(
    () =>
      kindFilter === "all"
        ? (["index", "industry", "concept"] as const)
        : ([kindFilter] as const),
    [kindFilter],
  )
  const totalCount = items.length
  const CurrentIcon = value ? KIND_META[value.target_type].icon : LineChart

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className={cn(
            "h-auto w-full justify-between rounded-xl border-slate-200 bg-white px-3 py-2.5 text-left text-slate-700 sm:w-[380px]",
            className,
          )}
        >
          <span className="flex min-w-0 items-center gap-2.5">
            <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-600">
              <CurrentIcon className="size-4" />
            </span>
            <span className="min-w-0">
              <span className="block truncate text-sm font-medium">
                {value ? value.name : "搜索板块 / 指数"}
              </span>
              <span className="block truncate text-xs text-slate-500">
                {value ? `${KIND_META[value.target_type].label} · ${value.code}` : "名称 / 代码"}
              </span>
            </span>
          </span>
          <ChevronsUpDown className="ml-2 size-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-[min(92vw,420px)] p-0">
        <Command>
          <CommandInput placeholder="输入名称或代码" />
          <div className="flex items-center gap-1 border-b border-slate-100 px-2 py-2">
            {([
              { key: "all", label: `全部 ${totalCount}` },
              { key: "index", label: `指数 ${groupedItems.index.length}` },
              { key: "industry", label: `行业 ${groupedItems.industry.length}` },
              { key: "concept", label: `概念 ${groupedItems.concept.length}` },
            ] as const).map((option) => (
              <button
                key={option.key}
                type="button"
                onClick={() => setKindFilter(option.key)}
                className={cn(
                  "inline-flex h-7 items-center rounded-md px-2.5 text-xs font-medium transition",
                  kindFilter === option.key
                    ? "bg-slate-900 text-white"
                    : "text-slate-500 hover:bg-slate-100 hover:text-slate-900",
                )}
              >
                {option.label}
              </button>
            ))}
          </div>
          <CommandList className="max-h-[min(70vh,520px)]">
            <CommandEmpty>没有匹配的板块 / 指数</CommandEmpty>
            {visibleKinds.map((kind) => {
              const group = groupedItems[kind]
              if (group.length === 0) return null
              return (
                <CommandGroup key={kind} heading={`${KIND_META[kind].commandGroup} (${group.length})`}>
                  {group.map((item) => {
                    const selected =
                      value?.code === item.code && value.target_type === item.kind
                    const Icon = KIND_META[item.kind].icon
                    return (
                      <CommandItem
                        key={`${item.kind}-${item.code}`}
                        value={`${item.name} ${item.code} ${KIND_META[item.kind].label}`}
                        onSelect={() => {
                          onSelect(item)
                          setOpen(false)
                        }}
                        className="gap-3 rounded-lg"
                      >
                        <span
                          className={cn(
                            "flex size-7 shrink-0 items-center justify-center rounded-lg",
                            item.kind === "industry" && "bg-blue-500/10 text-blue-700",
                            item.kind === "concept" && "bg-violet-500/10 text-violet-700",
                            item.kind === "index" && "bg-emerald-500/10 text-emerald-700",
                          )}
                        >
                          <Icon className="size-3.5" />
                        </span>
                        <div className="min-w-0 flex-1">
                          <div className="truncate text-sm font-medium text-slate-800">
                            {item.name}
                          </div>
                          <div className="flex items-center gap-2 text-xs text-slate-500">
                            <span className="font-mono">{item.code}</span>
                            <Badge
                              variant="outline"
                              className={cn("rounded-full", KIND_META[item.kind].badgeClassName)}
                            >
                              {KIND_META[item.kind].label}
                            </Badge>
                          </div>
                        </div>
                        <Check
                          className={cn(
                            "ml-auto size-4 text-slate-500",
                            selected ? "opacity-100" : "opacity-0",
                          )}
                        />
                      </CommandItem>
                    )
                  })}
                </CommandGroup>
              )
            })}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  )
}

export function industryApplicationTargetKindLabel(kind: IndustryApplicationTargetKind) {
  return KIND_META[kind].label
}
