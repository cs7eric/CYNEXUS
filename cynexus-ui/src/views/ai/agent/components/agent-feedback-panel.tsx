import { useEffect, useState } from "react"
import { Check, ThumbsDown, ThumbsUp } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { fetchAgentFeedback, fetchAgentFeedbackSummary, submitAgentFeedback, type AgentFeedback, type AgentFeedbackSummary } from "@/services/ai/agent"

export function AgentFeedbackPanel({ runNo }: { runNo: string }) {
  const [feedback, setFeedback] = useState<AgentFeedback | null>(null)
  const [summary, setSummary] = useState<AgentFeedbackSummary | null>(null)
  const [reason, setReason] = useState("")
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    void Promise.all([fetchAgentFeedback(runNo), fetchAgentFeedbackSummary()]).then(([nextFeedback, nextSummary]) => {
      if (!active) return
      setFeedback(nextFeedback)
      setSummary(nextSummary)
      setReason(nextFeedback?.feedbackReason ?? "")
    }).catch(() => { /* Feedback is optional and must not block the transcript. */ })
    return () => { active = false }
  }, [runNo])

  async function submit(rating: number) {
    setSaving(true)
    setMessage(null)
    try {
      const next = await submitAgentFeedback(runNo, {
        rating,
        feedbackReason: reason,
        evaluationLabel: rating >= 4 ? "CORRECT" : rating <= 2 ? "INCORRECT" : "PARTIAL",
      })
      setFeedback(next)
      setSummary(await fetchAgentFeedbackSummary())
      setMessage("Feedback recorded")
    } catch (error: unknown) {
      setMessage(error instanceof Error ? error.message : "Unable to record feedback")
    } finally {
      setSaving(false)
    }
  }

  return <div className="mt-5 border-t border-border/70 pt-4"><div className="flex flex-wrap items-center gap-2"><span className="text-xs text-muted-foreground">Was this answer useful?</span><Button size="icon" variant={feedback?.rating && feedback.rating >= 4 ? "secondary" : "ghost"} className="size-7" aria-label="Helpful answer" onClick={() => void submit(5)} disabled={saving}><ThumbsUp className="size-3.5" /></Button><Button size="icon" variant={feedback?.rating && feedback.rating <= 2 ? "secondary" : "ghost"} className="size-7" aria-label="Unhelpful answer" onClick={() => void submit(1)} disabled={saving}><ThumbsDown className="size-3.5" /></Button>{feedback ? <span className="inline-flex items-center gap-1 text-xs text-emerald-700"><Check className="size-3" /> Saved</span> : null}{message ? <span className="text-xs text-muted-foreground">{message}</span> : null}</div><Textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={120} placeholder="Optional correction reason" className="mt-2 min-h-16 resize-none text-xs" />{summary && summary.total > 0 ? <div className="mt-2 text-[11px] text-muted-foreground">Your feedback: {summary.total} total · {summary.averageRating.toFixed(1)}/5 average · {summary.reviewed} reviewed</div> : null}</div>
}
