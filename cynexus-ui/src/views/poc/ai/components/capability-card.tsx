import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import { aiStatusLabel, type AiCapability } from "../lib/capabilities"

const statusClassName = {
  ready: "border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/60 dark:bg-emerald-950/40 dark:text-emerald-300",
  wiring: "border-sky-200 bg-sky-50 text-sky-700 dark:border-sky-900/60 dark:bg-sky-950/40 dark:text-sky-300",
  planned: "border-zinc-200 bg-zinc-50 text-zinc-600 dark:border-zinc-800 dark:bg-zinc-900/50 dark:text-zinc-300",
} satisfies Record<AiCapability["status"], string>

interface CapabilityCardProps {
  capability: AiCapability
}

export function CapabilityCard({ capability }: CapabilityCardProps) {
  const Icon = capability.icon

  return (
    <article className="group flex min-h-[172px] flex-col justify-between rounded-lg bg-muted/35 p-4 transition-colors hover:bg-muted/55">
      <div className="space-y-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex size-10 items-center justify-center rounded-md bg-background text-foreground shadow-xs">
            <Icon className="size-4" />
          </div>
          <Badge
            variant="outline"
            className={cn("rounded-md px-2 py-0.5 text-[11px]", statusClassName[capability.status])}
          >
            {aiStatusLabel[capability.status]}
          </Badge>
        </div>
        <div className="space-y-1.5">
          <h2 className="text-sm font-semibold text-foreground">{capability.title}</h2>
          <p className="text-xs leading-5 text-muted-foreground">{capability.description}</p>
        </div>
      </div>
      <div className="mt-4 flex flex-wrap gap-1.5">
        {capability.anchors.map((anchor) => (
          <span
            key={anchor}
            className="rounded-md bg-background px-2 py-1 text-[11px] font-medium text-muted-foreground"
          >
            {anchor}
          </span>
        ))}
      </div>
    </article>
  )
}
