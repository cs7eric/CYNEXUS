import { useEffect, useState } from "react"
import { CalendarClock, CirclePause, Play, RefreshCw, Square } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { controlAgentTask, fetchAgentTasks, scheduleAgentTask, type AgentTask } from "@/services/ai/agent"

export function AgentTaskPanel() {
  const [tasks, setTasks] = useState<AgentTask[]>([])
  const [input, setInput] = useState("")
  const [scheduleAt, setScheduleAt] = useState("")
  const [message, setMessage] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  async function reload() { try { setTasks(await fetchAgentTasks()) } catch { setMessage("Unable to load scheduled tasks") } }
  useEffect(() => { void reload() }, [])

  async function schedule() {
    setSaving(true)
    try { await scheduleAgentTask({ userInput: input, scheduleAt: new Date(scheduleAt).toISOString(), maxAttempts: 3 }); setInput(""); setScheduleAt(""); setMessage("Task queued"); await reload() }
    catch (error: unknown) { setMessage(error instanceof Error ? error.message : "Unable to schedule task") }
    finally { setSaving(false) }
  }

  async function control(taskNo: string, action: "pause" | "resume" | "cancel") {
    try { await controlAgentTask(taskNo, action); await reload() } catch (error: unknown) { setMessage(error instanceof Error ? error.message : "Unable to update task") }
  }

  return <section className="rounded-xl border border-border/70 bg-card p-4"><div className="mb-3 flex items-center justify-between"><h2 className="flex items-center gap-2 text-sm font-semibold"><CalendarClock className="size-3.5 text-sky-600" /> Scheduled Agent work</h2><Button variant="ghost" size="icon" className="size-7" aria-label="Refresh tasks" onClick={() => void reload()}><RefreshCw className="size-3.5" /></Button></div><div className="space-y-2"><Textarea value={input} onChange={(event) => setInput(event.target.value)} placeholder="Task request" className="min-h-16 resize-none text-xs" /><Input type="datetime-local" value={scheduleAt} onChange={(event) => setScheduleAt(event.target.value)} className="text-xs" /><Button size="sm" variant="outline" onClick={() => void schedule()} disabled={saving || !input.trim() || !scheduleAt}>Queue task</Button></div>{tasks.length ? <div className="mt-4 space-y-2">{tasks.map((task) => <div key={task.taskNo} className="rounded-lg border border-border/70 bg-background p-2 text-xs"><div className="flex items-center justify-between gap-2"><span className="truncate font-medium">{task.userInput}</span><span className="shrink-0 text-muted-foreground">{task.status}</span></div><div className="mt-1 text-[11px] text-muted-foreground">{task.taskNo} · {new Date(task.scheduleAt).toLocaleString()}</div>{task.lastError ? <div className="mt-1 text-destructive">{task.lastError}</div> : null}{task.status === "QUEUED" ? <div className="mt-2 flex gap-1.5"><Button size="icon" variant="ghost" className="size-7" aria-label="Pause task" onClick={() => void control(task.taskNo, "pause")}><CirclePause className="size-3.5" /></Button><Button size="icon" variant="ghost" className="size-7" aria-label="Cancel task" onClick={() => void control(task.taskNo, "cancel")}><Square className="size-3.5" /></Button></div> : task.status === "PAUSED" ? <Button size="icon" variant="ghost" className="mt-2 size-7" aria-label="Resume task" onClick={() => void control(task.taskNo, "resume")}><Play className="size-3.5" /></Button> : null}</div>)}</div> : null}{message ? <div className="mt-2 text-xs text-muted-foreground">{message}</div> : null}</section>
}
