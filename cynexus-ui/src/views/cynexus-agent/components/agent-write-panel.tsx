import { useState } from "react"
import { Check, FilePlus2, ShieldCheck, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Input } from "@/components/ui/input"
import { confirmAgentWrite, previewAgentWrite, rejectAgentWrite, type AgentWritePreview } from "@/services/ai/agent"

export function AgentWritePanel({ conversationNo, runNo }: { conversationNo?: string | null; runNo?: string | null }) {
  const [title, setTitle] = useState("")
  const [content, setContent] = useState("")
  const [preview, setPreview] = useState<AgentWritePreview | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  async function createPreview() {
    setSaving(true)
    setMessage(null)
    try {
      setPreview(await previewAgentWrite({ toolCode: "knowledge.document.create", conversationNo: conversationNo ?? undefined, runNo: runNo ?? undefined, argumentsJson: JSON.stringify({ title, content, contentFormat: "markdown", status: "DRAFT" }) }))
    } catch (error: unknown) {
      setMessage(error instanceof Error ? error.message : "Unable to create write preview")
    } finally {
      setSaving(false)
    }
  }

  async function confirm() {
    if (!preview) return
    setSaving(true)
    try {
      const result = await confirmAgentWrite(preview.commandNo, preview.confirmationToken)
      setMessage(result.status === "EXECUTED" ? "Draft document created" : result.errorMessage ?? result.status)
      if (result.status === "EXECUTED") setPreview(null)
    } catch (error: unknown) {
      setMessage(error instanceof Error ? error.message : "Unable to confirm write")
    } finally {
      setSaving(false)
    }
  }

  async function reject() {
    if (!preview) return
    setSaving(true)
    try { await rejectAgentWrite(preview.commandNo); setPreview(null); setMessage("Write rejected") }
    catch (error: unknown) { setMessage(error instanceof Error ? error.message : "Unable to reject write") }
    finally { setSaving(false) }
  }

  return <section className="rounded-xl border border-border/70 bg-card p-4"><div className="mb-3 flex items-center gap-2"><FilePlus2 className="size-3.5 text-sky-600" /><h2 className="text-sm font-semibold">Confirmed write</h2></div><p className="mb-3 text-xs leading-5 text-muted-foreground">Creates a Knowledge Base draft only after an explicit confirmation.</p>{preview ? <div className="space-y-3 rounded-lg border border-amber-300/70 bg-amber-50/60 p-3 text-xs"><div className="flex items-center gap-2 font-medium text-amber-950"><ShieldCheck className="size-3.5" /> Review before creating draft</div><pre className="max-h-28 overflow-auto whitespace-pre-wrap rounded bg-background/80 p-2 text-[10px]">{preview.argumentsJson}</pre><div className="flex gap-2"><Button size="sm" onClick={() => void confirm()} disabled={saving}><Check className="size-3.5" /> Confirm</Button><Button size="sm" variant="outline" onClick={() => void reject()} disabled={saving}><X className="size-3.5" /> Reject</Button></div></div> : <div className="space-y-2"><Input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Draft title" maxLength={240} /><Textarea value={content} onChange={(event) => setContent(event.target.value)} placeholder="Draft content" maxLength={50000} className="min-h-24 resize-y text-xs" /><Button size="sm" variant="outline" onClick={() => void createPreview()} disabled={saving || !title.trim() || !content.trim()}>Preview write</Button></div>}{message ? <div className="mt-2 text-xs text-muted-foreground">{message}</div> : null}</section>
}
