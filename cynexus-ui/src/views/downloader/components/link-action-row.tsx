import { Check, Copy, ExternalLink } from "lucide-react"

import { Button } from "@/components/ui/button"
import { summarizeUrl } from "../lib/format"

export function LinkActionRow({
  label,
  value,
  copied,
  onCopy,
}: {
  label: string
  value: string | null | undefined
  copied: boolean
  onCopy: (value: string) => void
}) {
  return (
    <div className="grid gap-2 rounded-md bg-background px-3 py-2.5 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
      <div className="min-w-0 flex-1">
        <div className="text-xs font-medium uppercase text-muted-foreground">{label}</div>
        <div className="mt-0.5 truncate font-mono text-xs text-foreground/80">
          {value ? summarizeUrl(value) : "当前没有可用链接"}
        </div>
      </div>
      <div className="flex shrink-0 items-center justify-end gap-1.5">
        <Button type="button" variant="ghost" size="sm" className="h-8 rounded-md px-2 text-xs" disabled={!value} onClick={() => value && onCopy(value)}>
          {copied ? <Check className="size-4" /> : <Copy className="size-4" />}
          {copied ? "已复制" : "Copy"}
        </Button>
        <Button asChild variant="outline" size="sm" className="h-8 rounded-md px-2 text-xs" disabled={!value}>
          <a href={value || "#"} target="_blank" rel="noreferrer">
            <ExternalLink className="size-4" />
            Open
          </a>
        </Button>
      </div>
    </div>
  )
}
