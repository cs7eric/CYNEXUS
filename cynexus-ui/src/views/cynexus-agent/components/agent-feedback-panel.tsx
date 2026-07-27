import { useEffect, useState, type ReactNode } from "react"
import { Check, Clock3, Cpu, Sparkles, ThumbsDown, ThumbsUp } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  fetchAgentFeedback,
  submitAgentFeedback,
  type AgentRun,
  type AgentFeedback,
} from "@/services/ai/agent"
import { cn } from "@/lib/utils"

function formatProcessingTime(startedAt?: string | null, finishedAt?: string | null) {
  if (!startedAt || !finishedAt) return "--"
  const elapsedMs = Date.parse(finishedAt) - Date.parse(startedAt)
  if (!Number.isFinite(elapsedMs) || elapsedMs < 0) return "--"
  if (elapsedMs < 1000) return `${elapsedMs}ms`
  return `${(elapsedMs / 1000).toFixed(elapsedMs >= 10000 ? 1 : 2)}s`
}

export function AgentFeedbackPanel({
  run,
  runNo,
  actions,
}: {
  run: AgentRun
  runNo: string
  actions?: ReactNode
}) {
  const [feedback, setFeedback] = useState<AgentFeedback | null>(null)
  const [reason, setReason] = useState("")
  const [showReason, setShowReason] = useState(false)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    void fetchAgentFeedback(runNo)
      .then((nextFeedback) => {
        if (!active) return
        setFeedback(nextFeedback)
        setReason(nextFeedback?.feedbackReason ?? "")
        setShowReason(Boolean(nextFeedback?.feedbackReason) || Boolean(nextFeedback?.rating && nextFeedback.rating <= 2))
      })
      .catch(() => {
        /* Feedback is optional and must not block the transcript. */
      })
    return () => {
      active = false
    }
  }, [runNo])

  async function submit(rating: number, nextReason = reason) {
    setSaving(true)
    setMessage(null)
    try {
      const next = await submitAgentFeedback(runNo, {
        rating,
        feedbackReason: nextReason,
        evaluationLabel: rating >= 4 ? "CORRECT" : rating <= 2 ? "INCORRECT" : "PARTIAL",
      })
      setFeedback(next)
      setMessage("Saved")
      setShowReason(rating <= 2 || Boolean(nextReason.trim()))
    } catch (error: unknown) {
      setMessage(error instanceof Error ? error.message : "Unable to record feedback")
    } finally {
      setSaving(false)
    }
  }

  const helpful = Boolean(feedback?.rating && feedback.rating >= 4)
  const unhelpful = Boolean(feedback?.rating && feedback.rating <= 2)
  const processingTime = formatProcessingTime(run.startedAt, run.finishedAt)
  const reasoningEffort = run.reasoningEffort?.trim() || "low"

  return (
    <div className="mt-3 w-full min-w-0 space-y-2">
      <div className="flex w-full min-w-0 flex-wrap items-center gap-x-3 gap-y-1 px-1 text-[11px] text-muted-foreground">
        <span className="inline-flex min-w-0 items-center gap-1" title="Processing time">
          <Clock3 className="size-3 shrink-0" />
          <span>用时 {processingTime}</span>
        </span>
        <span className="inline-flex min-w-0 max-w-full items-center gap-1" title={run.modelCode || "Model unavailable"}>
          <Cpu className="size-3 shrink-0" />
          <span className="max-w-[12rem] truncate">{run.modelCode || "Model unavailable"}</span>
        </span>
        <span className="inline-flex items-center gap-1" title="Reasoning level">
          <Sparkles className="size-3 shrink-0" />
          <span>Reasoning {reasoningEffort}</span>
        </span>
      </div>
      <div className="inline-flex max-w-full flex-wrap items-center gap-0.5 p-0">
        <Button
          size="icon"
          variant="ghost"
          className={cn("size-7 rounded-md text-muted-foreground hover:text-foreground", helpful && "text-foreground")}
          aria-label="Helpful answer"
          title="Helpful"
          onClick={() => void submit(5)}
          disabled={saving}
        >
          <ThumbsUp className="size-3.5" />
        </Button>
        <Button
          size="icon"
          variant="ghost"
          className={cn("size-7 rounded-md text-muted-foreground hover:text-foreground", unhelpful && "text-foreground")}
          aria-label="Unhelpful answer"
          title="Unhelpful"
          onClick={() => {
            setShowReason(true)
            void submit(1)
          }}
          disabled={saving}
        >
          <ThumbsDown className="size-3.5" />
        </Button>

        {actions ? (
          <>
            <span className="mx-0.5 h-4 w-px shrink-0 bg-border" aria-hidden />
            <div className="flex items-center gap-0.5">{actions}</div>
          </>
        ) : null}

        {feedback || message ? (
          <span className="inline-flex items-center gap-1 px-1.5 text-[11px] text-muted-foreground">
            <Check className="size-3 text-emerald-600" />
            {message ?? "Saved"}
          </span>
        ) : null}
      </div>

      {showReason ? (
        <div className="flex max-w-md items-center gap-2 pl-1">
          <Input
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            maxLength={120}
            placeholder="What was wrong?"
            className="h-8 border-0 bg-transparent px-0 text-xs shadow-none focus-visible:border-0 focus-visible:ring-0"
            autoFocus
            onKeyDown={(event) => {
              if (event.key !== "Enter" || event.nativeEvent.isComposing) return
              event.preventDefault()
              if (feedback?.rating) void submit(feedback.rating, reason)
              else void submit(1, reason)
            }}
          />
          <Button
            size="sm"
            variant="ghost"
            className="h-8 shrink-0 px-2 text-[11px]"
            disabled={saving || !reason.trim()}
            onClick={() => void submit(feedback?.rating ?? 1, reason)}
          >
            Save
          </Button>
        </div>
      ) : null}
    </div>
  )
}
