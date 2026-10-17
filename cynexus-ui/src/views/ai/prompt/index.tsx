import { useCallback, useEffect, useMemo, useState } from "react"
import { BrainCircuit, FileCode2, Plus, RefreshCw, Save, Search, Trash2 } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { notification } from "@/components/ui/notification"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { UnsavedChangesGuard } from "@/components/unsaved-changes-guard"
import { cn } from "@/lib/utils"
import {
  type AiPromptItem,
  type AiPromptStatus,
  createAiPrompt,
  deleteAiPrompt,
  fetchAiPrompts,
  updateAiPrompt,
} from "@/services/ai/prompt"

/**
 * Maintenance Notice:
 * Before modifying this page, review the related documentation under:
 * - design/front/infra/index.md
 * - design/front/infra/ai-prompt.detail.md
 * - design/front/reuse/reuse.md
 *
 * If this page's route, data source, API usage, component structure,
 * or reusable logic changes, update the corresponding design documents.
 */

type PromptDraft = Omit<AiPromptItem, "id" | "created_at" | "updated_at"> & { id?: string }

const emptyDraft: PromptDraft = {
  prompt_key: "",
  name: "",
  scenario_code: "",
  prompt_version: "v1",
  status: "draft",
  system_prompt: "",
  user_prompt_template: "",
  output_schema: "",
  variables: "[]",
  tags: "[]",
  extra: "{}",
  remark: "",
}

const statusOptions: Array<{ value: AiPromptStatus; label: string; className: string }> = [
  { value: "active", label: "Active", className: "bg-emerald-500/12 text-emerald-700 border-emerald-200" },
  { value: "draft", label: "Draft", className: "bg-slate-500/10 text-slate-700 border-slate-200" },
  { value: "archived", label: "Archived", className: "bg-amber-500/12 text-amber-700 border-amber-200" },
]

function draftFromPrompt(prompt: AiPromptItem): PromptDraft {
  return {
    id: prompt.id,
    prompt_key: prompt.prompt_key,
    name: prompt.name,
    scenario_code: prompt.scenario_code,
    prompt_version: prompt.prompt_version,
    status: prompt.status,
    system_prompt: prompt.system_prompt,
    user_prompt_template: prompt.user_prompt_template,
    output_schema: prompt.output_schema,
    variables: prompt.variables,
    tags: prompt.tags,
    extra: prompt.extra,
    remark: prompt.remark,
  }
}

function formatDate(value?: string | null) {
  if (!value) return "-"
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value))
}

function validateJsonField(label: string, value: string, requiredType: "array" | "object" | "any") {
  if (!value.trim()) return
  const parsed = JSON.parse(value)
  if (requiredType === "array" && !Array.isArray(parsed)) {
    throw new Error(`${label} 必须是 JSON 数组`)
  }
  if (requiredType === "object" && (!parsed || typeof parsed !== "object" || Array.isArray(parsed))) {
    throw new Error(`${label} 必须是 JSON 对象`)
  }
}

function statusBadge(status: AiPromptStatus) {
  const option = statusOptions.find((item) => item.value === status)
  return <Badge variant="outline" className={cn("border font-medium", option?.className)}>{option?.label || status}</Badge>
}

export default function PromptSettingsPage() {
  const [prompts, setPrompts] = useState<AiPromptItem[]>([])
  const [keyword, setKeyword] = useState("")
  const [status, setStatus] = useState<AiPromptStatus | "all">("all")
  const [scenarioCode, setScenarioCode] = useState("")
  const [draft, setDraft] = useState<PromptDraft>(emptyDraft)
  const [initialDraft, setInitialDraft] = useState<PromptDraft>(emptyDraft)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [dialogMode, setDialogMode] = useState<"create" | "edit">("create")
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState("")

  const reload = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      const nextPrompts = await fetchAiPrompts({ keyword, scenario_code: scenarioCode, status })
      setPrompts(nextPrompts)
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载 Prompt 失败")
    } finally {
      setLoading(false)
    }
  }, [keyword, scenarioCode, status])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void reload()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [reload])

  const stats = useMemo(() => {
    return prompts.reduce(
      (acc, prompt) => {
        acc.total += 1
        acc[prompt.status] += 1
        return acc
      },
      { total: 0, active: 0, draft: 0, archived: 0 },
    )
  }, [prompts])

  const scenarios = useMemo(() => {
    return Array.from(new Set(prompts.map((item) => item.scenario_code).filter(Boolean))).sort()
  }, [prompts])

  function openCreateDialog() {
    setDialogMode("create")
    setDraft(emptyDraft)
    setInitialDraft(emptyDraft)
    setDialogOpen(true)
  }

  function openEditDialog(prompt: AiPromptItem) {
    setDialogMode("edit")
    setDraft(draftFromPrompt(prompt))
    setInitialDraft(draftFromPrompt(prompt))
    setDialogOpen(true)
  }

  async function handleSavePrompt() {
    if (!draft.prompt_key.trim() || !draft.name.trim() || !draft.system_prompt.trim()) {
      notification.danger({ title: "Prompt 未保存", description: "Key、名称和 System prompt 不能为空" })
      return
    }
    try {
      validateJsonField("Output schema", draft.output_schema, "any")
      validateJsonField("Variables", draft.variables, "array")
      validateJsonField("Tags", draft.tags, "array")
      validateJsonField("Extra", draft.extra, "object")
    } catch (err) {
      notification.danger({ title: "JSON 格式错误", description: err instanceof Error ? err.message : "JSON 无效" })
      return
    }

    setSaving(true)
    try {
      const saved = draft.id ? await updateAiPrompt(draft.id, draft) : await createAiPrompt(draft)
      notification.success({ title: "Prompt 已保存", description: saved.name })
      setDialogOpen(false)
      await reload()
    } catch (err) {
      notification.danger({ title: "Prompt 保存失败", description: err instanceof Error ? err.message : "保存失败" })
    } finally {
      setSaving(false)
    }
  }

  async function handleDeletePrompt() {
    if (!draft.id) return
    setSaving(true)
    try {
      await deleteAiPrompt(draft.id)
      notification.success({ title: "Prompt 已删除", description: draft.name })
      setDialogOpen(false)
      await reload()
    } catch (err) {
      notification.danger({ title: "删除失败", description: err instanceof Error ? err.message : "删除失败" })
    } finally {
      setSaving(false)
    }
  }

  return (
    <WorkspaceShell>
      <UnsavedChangesGuard when={dialogOpen && JSON.stringify(draft) !== JSON.stringify(initialDraft)} saving={saving} />
      <div className="flex w-full flex-col gap-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <BrainCircuit className="size-4" />
              AI
            </div>
            <h1 className="text-2xl font-semibold tracking-tight text-foreground">Prompt</h1>
            <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
              管理 AI 场景使用的 system prompt、user template、变量和输出约束。
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={() => void reload()} disabled={loading}>
              <RefreshCw className={cn("size-4", loading && "animate-spin")} />
              Reload
            </Button>
            <Button variant="secondary" onClick={openCreateDialog}>
              <Plus className="size-4" />
              New prompt
            </Button>
          </div>
        </div>

        {error ? (
          <Alert variant="destructive">
            <AlertTitle>Prompt 加载失败</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        ) : null}

        <section className="grid gap-3 md:grid-cols-4">
          <div className="rounded-lg bg-muted/50 p-4">
            <div className="text-xs font-medium text-muted-foreground">Total</div>
            <div className="mt-2 text-2xl font-semibold">{stats.total}</div>
          </div>
          <div className="rounded-lg bg-muted/50 p-4">
            <div className="text-xs font-medium text-muted-foreground">Active</div>
            <div className="mt-2 text-2xl font-semibold text-emerald-700">{stats.active}</div>
          </div>
          <div className="rounded-lg bg-muted/50 p-4">
            <div className="text-xs font-medium text-muted-foreground">Draft</div>
            <div className="mt-2 text-2xl font-semibold">{stats.draft}</div>
          </div>
          <div className="rounded-lg bg-muted/50 p-4">
            <div className="text-xs font-medium text-muted-foreground">Archived</div>
            <div className="mt-2 text-2xl font-semibold text-amber-700">{stats.archived}</div>
          </div>
        </section>

        <section className="space-y-3">
          <div className="flex flex-col gap-2 rounded-lg bg-muted/50 p-3 lg:flex-row lg:items-center">
            <div className="relative min-w-[240px] flex-1">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Search prompt key / name / scenario"
                className="pl-9"
              />
            </div>
            <Select value={status} onValueChange={(value) => setStatus(value as AiPromptStatus | "all")}>
              <SelectTrigger className="w-full lg:w-[160px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All status</SelectItem>
                {statusOptions.map((item) => (
                  <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select value={scenarioCode || "all"} onValueChange={(value) => setScenarioCode(value === "all" ? "" : value)}>
              <SelectTrigger className="w-full lg:w-[220px]">
                <SelectValue placeholder="Scenario" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All scenarios</SelectItem>
                {scenarios.map((scenario) => (
                  <SelectItem key={scenario} value={scenario}>{scenario}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="overflow-hidden rounded-lg bg-muted/50">
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead className="w-[28%] px-4">Prompt</TableHead>
                  <TableHead>Scenario</TableHead>
                  <TableHead>Version</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Updated</TableHead>
                  <TableHead className="pr-4 text-right">Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {prompts.map((prompt) => (
                  <TableRow key={prompt.id} className="cursor-pointer" onClick={() => openEditDialog(prompt)}>
                    <TableCell className="px-4">
                      <div className="flex min-w-0 items-center gap-2">
                        <FileCode2 className="size-4 shrink-0 text-muted-foreground" />
                        <div className="min-w-0">
                          <div className="truncate font-medium">{prompt.name}</div>
                          <div className="truncate text-xs text-muted-foreground">{prompt.prompt_key}</div>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{prompt.scenario_code || "-"}</TableCell>
                    <TableCell>{prompt.prompt_version}</TableCell>
                    <TableCell>{statusBadge(prompt.status)}</TableCell>
                    <TableCell className="text-muted-foreground">{formatDate(prompt.updated_at || prompt.created_at)}</TableCell>
                    <TableCell className="pr-4 text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(event) => {
                          event.stopPropagation()
                          openEditDialog(prompt)
                        }}
                      >
                        Edit
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
                {!prompts.length && !loading ? (
                  <TableRow>
                    <TableCell colSpan={6} className="h-28 text-center text-muted-foreground">
                      暂无 prompt，点击 New prompt 创建。
                    </TableCell>
                  </TableRow>
                ) : null}
              </TableBody>
            </Table>
          </div>
        </section>

        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogContent className="max-h-[calc(100vh-2rem)] w-[calc(100vw-2rem)] overflow-y-auto sm:!max-w-[92rem]">
            <DialogHeader>
              <DialogTitle>{dialogMode === "create" ? "New prompt" : "Edit prompt"}</DialogTitle>
              <DialogDescription>
                Prompt key 和 version 组合唯一，active 状态用于线上场景读取。
              </DialogDescription>
            </DialogHeader>

            <div className="grid gap-4 lg:grid-cols-4">
              <div className="space-y-2">
                <Label>Prompt key</Label>
                <Input value={draft.prompt_key} onChange={(event) => setDraft({ ...draft, prompt_key: event.target.value })} placeholder="market_daily_review" />
              </div>
              <div className="space-y-2">
                <Label>Name</Label>
                <Input value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} placeholder="Market daily review" />
              </div>
              <div className="space-y-2">
                <Label>Scenario code</Label>
                <Input value={draft.scenario_code} onChange={(event) => setDraft({ ...draft, scenario_code: event.target.value })} placeholder="market_daily_review" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label>Version</Label>
                  <Input value={draft.prompt_version} onChange={(event) => setDraft({ ...draft, prompt_version: event.target.value })} placeholder="v1" />
                </div>
                <div className="space-y-2">
                  <Label>Status</Label>
                  <Select value={draft.status} onValueChange={(value) => setDraft({ ...draft, status: value as AiPromptStatus })}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {statusOptions.map((item) => (
                        <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-2 lg:col-span-2">
                <Label>System prompt</Label>
                <textarea
                  value={draft.system_prompt}
                  onChange={(event) => setDraft({ ...draft, system_prompt: event.target.value })}
                  className="min-h-[260px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring/40"
                  placeholder="You are..."
                />
              </div>
              <div className="space-y-2 lg:col-span-2">
                <Label>User prompt template</Label>
                <textarea
                  value={draft.user_prompt_template}
                  onChange={(event) => setDraft({ ...draft, user_prompt_template: event.target.value })}
                  className="min-h-[260px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring/40"
                  placeholder="Input: {{content}}"
                />
              </div>
              <div className="space-y-2 lg:col-span-2">
                <Label>Output schema</Label>
                <textarea
                  value={draft.output_schema}
                  onChange={(event) => setDraft({ ...draft, output_schema: event.target.value })}
                  className="min-h-[150px] w-full rounded-md border border-input bg-background px-3 py-2 font-mono text-xs outline-none focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring/40"
                  placeholder='{"type":"object"}'
                />
              </div>
              <div className="grid gap-4 lg:col-span-2 lg:grid-cols-2">
                <div className="space-y-2">
                  <Label>Variables</Label>
                  <textarea
                    value={draft.variables}
                    onChange={(event) => setDraft({ ...draft, variables: event.target.value })}
                    className="min-h-[150px] w-full rounded-md border border-input bg-background px-3 py-2 font-mono text-xs outline-none focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring/40"
                    placeholder='["content"]'
                  />
                </div>
                <div className="space-y-2">
                  <Label>Tags</Label>
                  <textarea
                    value={draft.tags}
                    onChange={(event) => setDraft({ ...draft, tags: event.target.value })}
                    className="min-h-[150px] w-full rounded-md border border-input bg-background px-3 py-2 font-mono text-xs outline-none focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring/40"
                    placeholder='["market"]'
                  />
                </div>
              </div>
              <div className="space-y-2 lg:col-span-2">
                <Label>Extra</Label>
                <textarea
                  value={draft.extra}
                  onChange={(event) => setDraft({ ...draft, extra: event.target.value })}
                  className="min-h-[110px] w-full rounded-md border border-input bg-background px-3 py-2 font-mono text-xs outline-none focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring/40"
                  placeholder="{}"
                />
              </div>
              <div className="space-y-2 lg:col-span-2">
                <Label>Remark</Label>
                <textarea
                  value={draft.remark}
                  onChange={(event) => setDraft({ ...draft, remark: event.target.value })}
                  className="min-h-[110px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring/40"
                  placeholder="说明这个 prompt 在哪里使用"
                />
              </div>
            </div>

            <DialogFooter className="gap-2">
              {dialogMode === "edit" && draft.id ? (
                <Button variant="destructive" onClick={() => void handleDeletePrompt()} disabled={saving}>
                  <Trash2 className="size-4" />
                  Delete
                </Button>
              ) : null}
              <Button onClick={() => void handleSavePrompt()} disabled={saving}>
                {saving ? <RefreshCw className="size-4 animate-spin" /> : <Save className="size-4" />}
                Save prompt
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </WorkspaceShell>
  )
}
