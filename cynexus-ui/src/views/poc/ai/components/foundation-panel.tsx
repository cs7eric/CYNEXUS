import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { cn } from "@/lib/utils"
import { aiFoundationItems, aiTracks } from "../lib/capabilities"

const toneClassName = {
  ok: "bg-emerald-500",
  warn: "bg-sky-500",
  idle: "bg-zinc-400",
} satisfies Record<(typeof aiFoundationItems)[number]["tone"], string>

export function FoundationPanel() {
  return (
    <aside className="rounded-lg bg-muted/35 p-4">
      <div className="space-y-1">
        <p className="text-xs font-medium uppercase text-muted-foreground">AI POC lane</p>
        <h2 className="text-lg font-semibold tracking-tight text-foreground">开发能力底座</h2>
      </div>

      <div className="mt-4 grid gap-2">
        {aiFoundationItems.map((item) => (
          <div
            key={item.label}
            className="flex items-center justify-between gap-3 rounded-md bg-background px-3 py-2"
          >
            <div className="flex min-w-0 items-center gap-2">
              <span className={cn("size-2 rounded-full", toneClassName[item.tone])} />
              <span className="truncate text-xs font-medium text-foreground">{item.label}</span>
            </div>
            <Badge variant="secondary" className="rounded-md text-[11px]">
              {item.value}
            </Badge>
          </div>
        ))}
      </div>

      <Separator className="my-4 bg-border/50" />

      <div className="grid gap-3">
        {aiTracks.map((track) => {
          const Icon = track.icon
          return (
            <div key={track.title} className="flex gap-3">
              <div className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-md bg-background text-foreground shadow-xs">
                <Icon className="size-4" />
              </div>
              <div className="min-w-0 space-y-0.5">
                <h3 className="text-sm font-medium text-foreground">{track.title}</h3>
                <p className="text-xs leading-5 text-muted-foreground">{track.description}</p>
              </div>
            </div>
          )
        })}
      </div>
    </aside>
  )
}
