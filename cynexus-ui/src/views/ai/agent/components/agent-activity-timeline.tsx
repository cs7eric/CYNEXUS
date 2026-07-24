import { useState } from "react"
import { Check, ChevronDown, ChevronRight, MinusCircle, Wrench } from "lucide-react"
import { cn } from "@/lib/utils"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { Marker, MarkerContent, MarkerIcon, markerVariants } from "@/components/ui/marker"
import { Message, MessageContent } from "@/components/ui/message"
import { Spinner } from "@/components/ui/spinner"
import type { AgentRunStep } from "@/services/ai/agent"

export type AgentTimelineEntry =
  | {
      id: string
      type: "thinking"
      text: string
      done: boolean
    }
  | {
      id: string
      type: "tool"
      stepNo?: number | null
      toolCode: string
      summary: string
      status: string
    }

function lastMeaningfulLine(text: string) {
  const lines = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
  return lines.at(-1) ?? ""
}

function thinkingTextFromStep(step: AgentRunStep) {
  if (step.outputJson) {
    try {
      const parsed = JSON.parse(step.outputJson) as { content?: string }
      if (parsed?.content) return parsed.content
    } catch {
      // fall through
    }
  }
  return step.summary ?? ""
}

export function timelineFromSteps(steps?: AgentRunStep[] | null): AgentTimelineEntry[] {
  if (!steps?.length) return []
  const entries: AgentTimelineEntry[] = []
  for (const step of steps) {
    if (step.kind === "THINKING") {
      const text = thinkingTextFromStep(step)
      if (!text.trim()) continue
      entries.push({
        id: `thinking-${step.stepNo ?? entries.length}`,
        type: "thinking",
        text,
        done: true,
      })
      continue
    }
    if (step.kind === "TOOL" || step.toolCode) {
      entries.push({
        id: `tool-${step.stepNo ?? entries.length}-${step.toolCode ?? "tool"}`,
        type: "tool",
        stepNo: step.stepNo,
        toolCode: step.toolCode ?? "tool",
        summary: step.summary ?? "",
        status: step.status ?? "SUCCEEDED",
      })
    }
  }
  return entries
}

export function appendThinkingDelta(entries: AgentTimelineEntry[], delta: string): AgentTimelineEntry[] {
  if (!delta) return entries
  const next = [...entries]
  const last = next.at(-1)
  if (last?.type === "thinking" && !last.done) {
    next[next.length - 1] = { ...last, text: `${last.text}${delta}` }
    return next
  }
  next.push({
    id: `thinking-live-${Date.now()}`,
    type: "thinking",
    text: delta,
    done: false,
  })
  return next
}

export function completeThinking(entries: AgentTimelineEntry[], summary?: string | null): AgentTimelineEntry[] {
  const next = [...entries]
  const last = next.at(-1)
  if (last?.type === "thinking" && !last.done) {
    next[next.length - 1] = {
      ...last,
      text: last.text.trim() || summary || last.text,
      done: true,
    }
    return next
  }
  if (summary?.trim()) {
    next.push({
      id: `thinking-${Date.now()}`,
      type: "thinking",
      text: summary,
      done: true,
    })
  }
  return next
}

export function appendToolStep(
  entries: AgentTimelineEntry[],
  tool: { stepNo?: number | null; toolCode?: string | null; summary?: string | null; status?: string | null },
): AgentTimelineEntry[] {
  const toolCode = tool.toolCode?.trim() || "tool"
  const id = `tool-${tool.stepNo ?? Date.now()}-${toolCode}`
  if (entries.some((entry) => entry.id === id)) return entries
  return [
    ...entries,
    {
      id,
      type: "tool",
      stepNo: tool.stepNo,
      toolCode,
      summary: tool.summary ?? "",
      status: tool.status ?? "SUCCEEDED",
    },
  ]
}

function toolLabel(entry: Extract<AgentTimelineEntry, { type: "tool" }>) {
  const code = entry.toolCode
  if (entry.status === "FAILED") return `未能调用 ${code}`
  if (entry.status === "SKIPPED") return `已跳过 ${code}`
  if (entry.summary?.trim() && entry.summary.trim().length <= 64) return entry.summary.trim()
  return `已调用 ${code}`
}

function ThinkingRow({
  entry,
}: {
  entry: Extract<AgentTimelineEntry, { type: "thinking" }>
}) {
  const [open, setOpen] = useState(false)

  if (!entry.done) {
    const line = lastMeaningfulLine(entry.text) || "Thinking…"
    return (
      <Marker role="status" className="text-xs">
        <MarkerIcon>
          <Spinner className="size-3.5" />
        </MarkerIcon>
        <MarkerContent className="shimmer line-clamp-1 min-w-0">{line}</MarkerContent>
      </Marker>
    )
  }

  return (
    <Collapsible open={open} onOpenChange={setOpen} className="w-full min-w-0">
      <CollapsibleTrigger
        className={cn(markerVariants(), "cursor-pointer text-xs outline-none hover:text-foreground")}
      >
        <MarkerIcon>
          {open ? <ChevronDown className="size-3.5" /> : <ChevronRight className="size-3.5" />}
        </MarkerIcon>
        <MarkerContent>Thought</MarkerContent>
      </CollapsibleTrigger>
      <CollapsibleContent className="overflow-hidden data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0">
        <div className="mt-1 max-h-48 w-full min-w-0 overflow-auto whitespace-pre-wrap break-words pl-6 text-xs leading-5 text-muted-foreground">
          {entry.text}
        </div>
      </CollapsibleContent>
    </Collapsible>
  )
}

function ToolRow({ entry }: { entry: Extract<AgentTimelineEntry, { type: "tool" }> }) {
  const [open, setOpen] = useState(false)
  const failed = entry.status === "FAILED"
  const skipped = entry.status === "SKIPPED"
  const detail = entry.summary?.trim()

  return (
    <Collapsible open={open} onOpenChange={setOpen} className="w-full min-w-0">
      <CollapsibleTrigger
        className={cn(
          markerVariants(),
          "cursor-pointer text-xs outline-none",
          failed ? "text-red-700/75 hover:text-red-800/85 dark:text-red-400/75 dark:hover:text-red-300/85" : "text-muted-foreground hover:text-foreground",
        )}
      >
        <MarkerIcon>
          {failed ? (
            <span className="flex size-3.5 items-center justify-center text-[13px] font-semibold leading-none text-red-700/70 dark:text-red-400/70" aria-hidden>
              !
            </span>
          ) : skipped ? (
            <MinusCircle className="size-3.5 text-muted-foreground/80" />
          ) : (
            <Wrench className="size-3.5 text-muted-foreground" />
          )}
        </MarkerIcon>
        <MarkerContent className="line-clamp-1 min-w-0">{toolLabel(entry)}</MarkerContent>
        {detail ? (
          open ? <ChevronDown className="ml-auto size-3.5 shrink-0 opacity-60" /> : <ChevronRight className="ml-auto size-3.5 shrink-0 opacity-60" />
        ) : failed || skipped ? null : (
          <Check className="ml-auto size-3.5 shrink-0 opacity-40" />
        )}
      </CollapsibleTrigger>
      {detail ? (
        <CollapsibleContent className="overflow-hidden data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0">
          <div className="mt-1 w-full min-w-0 pl-6 text-[11px] leading-5 text-muted-foreground">
            <div className="truncate font-medium text-foreground/80">{entry.toolCode}</div>
            <div className="mt-0.5 whitespace-pre-wrap break-words">{detail}</div>
          </div>
        </CollapsibleContent>
      ) : null}
    </Collapsible>
  )
}

export function AgentActivityTimeline({
  entries,
}: {
  entries: AgentTimelineEntry[]
}) {
  if (!entries.length) return null

  return (
    <Message>
      <MessageContent className="gap-1.5">
        {entries.map((entry) => {
          if (entry.type === "thinking") {
            return <ThinkingRow key={entry.id} entry={entry} />
          }
          return <ToolRow key={entry.id} entry={entry} />
        })}
      </MessageContent>
    </Message>
  )
}
