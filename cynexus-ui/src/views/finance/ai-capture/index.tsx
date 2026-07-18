import { useCallback, useEffect, useMemo, useState } from "react"
import { Camera, Check, FileImage, LoaderCircle, Trash2, Upload, X } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { notification } from "@/components/ui/notification"
import { Skeleton } from "@/components/ui/skeleton"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { fetchFinanceAccounts, fetchFinanceCategories, createFinanceAiCapture, confirmFinanceAiCapture, type FinanceAccount, type FinanceAiCapture, type FinanceAiCaptureItem, type FinanceCategory } from "@/services/finance/ledger"
import { FinancePageHeader } from "../components/page-header"
import { formatMoney } from "../lib/format"

type Draft = FinanceAiCaptureItem

function editable(item: Draft, patch: Partial<Draft>) { return { ...item, ...patch } }

export default function FinanceAiCapturePage() {
  const [files, setFiles] = useState<File[]>([])
  const [capture, setCapture] = useState<FinanceAiCapture | null>(null)
  const [drafts, setDrafts] = useState<Draft[]>([])
  const [accounts, setAccounts] = useState<FinanceAccount[]>([])
  const [categories, setCategories] = useState<FinanceCategory[]>([])
  const [uploading, setUploading] = useState(false)
  const [confirming, setConfirming] = useState(false)

  useEffect(() => { void Promise.all([fetchFinanceAccounts(), fetchFinanceCategories()]).then(([nextAccounts, nextCategories]) => { setAccounts(nextAccounts); setCategories(nextCategories) }).catch((cause) => notification.danger({ title: "图片记账准备失败", description: cause instanceof Error ? cause.message : String(cause) })) }, [])

  const previews = useMemo(() => files.map((file) => ({ file, url: URL.createObjectURL(file) })), [files])
  useEffect(() => () => previews.forEach((item) => URL.revokeObjectURL(item.url)), [previews])

  const chooseFiles = useCallback((list: FileList | null) => {
    if (!list) return
    const selected = Array.from(list)
    if (selected.length > 10) { notification.warn({ title: "图片数量已限制", description: "单批最多上传 10 张图片。" }); setFiles(selected.slice(0, 10)); return }
    const invalid = selected.find((file) => !file.type.startsWith("image/") || file.size > 10 * 1024 * 1024)
    if (invalid) { notification.warn({ title: "图片不可用", description: `${invalid.name} 不是受支持的图片或超过 10MB。` }); return }
    setFiles(selected); setCapture(null); setDrafts([])
  }, [])

  async function parse() {
    if (!files.length) { notification.warn({ title: "请先选择图片" }); return }
    setUploading(true); setCapture(null); setDrafts([])
    try { const result = await createFinanceAiCapture(files); setCapture(result); setDrafts(result.items); if (result.status === "failed") notification.danger({ title: "图片识别失败", description: result.errorMessage || "模型没有返回可用结果。" }); else notification.success({ title: "识别完成", description: `识别到 ${result.items.length} 条待审核账目。` }) }
    catch (cause) { notification.danger({ title: "图片解析失败", description: cause instanceof Error ? cause.message : String(cause) }) }
    finally { setUploading(false) }
  }

  function update(id: number, patch: Partial<Draft>) { setDrafts((items) => items.map((item) => item.id === id ? editable(item, patch) : item)) }
  function reject(id: number) { update(id, { status: "rejected" }) }
  function restore(id: number) { update(id, { status: "draft" }) }

  async function confirm() {
    if (!capture) return
    const active = drafts.filter((item) => item.status !== "rejected")
    if (active.some((item) => !item.amount || !item.accountId || !item.categoryId || !item.occurredAt)) { notification.warn({ title: "草稿尚未完整", description: "请补全金额、日期、账户和分类后再入账。" }); return }
    setConfirming(true)
    try { const result = await confirmFinanceAiCapture(capture.id, drafts.map((item) => ({ id: item.id, transactionType: item.transactionType, occurredAt: item.occurredAt, amount: item.amount, currency: item.currency, merchant: item.merchant, categoryId: item.categoryId, accountId: item.accountId, note: item.note, status: item.status }))); setCapture(result); setDrafts(result.items); notification.success({ title: "已完成入账", description: "审核通过的草稿已写入账本。" }) }
    catch (cause) { notification.danger({ title: "入账失败", description: cause instanceof Error ? cause.message : String(cause) }) }
    finally { setConfirming(false) }
  }

  return <WorkspaceShell sectionLabel="Finance" pageTitle="AI Capture">
    <FinancePageHeader current="ai-capture" description="上传一张或多张小票、账单截图，先由 AI 生成草稿，再由你审核后入账。" />
    <div className="grid gap-4 xl:grid-cols-[minmax(0,0.78fr)_minmax(0,1.22fr)]">
      <section className="min-w-0 rounded-lg border border-border/70 bg-card p-4"><div className="mb-4 flex items-start justify-between gap-3"><div><h2 className="text-base font-semibold">上传凭证</h2><p className="mt-1 text-xs leading-5 text-muted-foreground">支持最多 10 张图片，每张不超过 10MB。识别结果只会进入待审核草稿。</p></div><Camera className="size-5 text-muted-foreground" /></div><label className="flex min-h-44 cursor-pointer flex-col items-center justify-center rounded-lg border border-dashed border-border bg-muted/20 px-5 text-center transition-colors hover:bg-muted/40"><FileImage className="size-8 text-muted-foreground" /><span className="mt-3 text-sm font-medium">选择小票或账单图片</span><span className="mt-1 text-xs text-muted-foreground">可以一次选择多张</span><input type="file" accept="image/*" multiple className="sr-only" onChange={(event) => chooseFiles(event.target.files)} /></label>{previews.length ? <div className="mt-4 grid grid-cols-3 gap-2">{previews.map(({ file, url }) => <div key={`${file.name}-${file.lastModified}`} className="group relative aspect-square overflow-hidden rounded-md border border-border/60 bg-muted"><img src={url} alt={file.name} className="size-full object-cover" /><button type="button" title="移除图片" onClick={() => setFiles((items) => items.filter((item) => item !== file))} className="absolute right-1 top-1 rounded-md bg-black/60 p-1 text-white opacity-0 transition-opacity group-hover:opacity-100"><X className="size-3.5" /></button></div>)}</div> : null}<div className="mt-4 flex items-center justify-between gap-2"><span className="text-xs text-muted-foreground">{files.length ? `${files.length} 张图片已选择` : "尚未选择图片"}</span><Button type="button" onClick={() => void parse()} disabled={!files.length || uploading} className="gap-2">{uploading ? <LoaderCircle className="size-4 animate-spin" /> : <Upload className="size-4" />}{uploading ? "识别中" : "开始识别"}</Button></div></section>
      <section className="min-w-0 rounded-lg border border-border/70 bg-card p-4"><div className="mb-4 flex items-start justify-between gap-3"><div><h2 className="text-base font-semibold">审核草稿</h2><p className="mt-1 text-xs text-muted-foreground">模型只负责提取信息，分类、账户和金额都可以在入账前修正。</p></div>{capture ? <span className="rounded-md bg-muted px-2 py-1 text-xs text-muted-foreground">{capture.status === "review" ? "待审核" : capture.status}</span> : null}</div>{uploading ? <div className="space-y-3"><Skeleton className="h-24" /><Skeleton className="h-24" /><Skeleton className="h-24" /></div> : drafts.length ? <div className="space-y-3">{drafts.map((item) => <DraftRow key={item.id} item={item} accounts={accounts} categories={categories} onUpdate={update} onReject={reject} onRestore={restore} />)}<div className="flex flex-wrap items-center justify-between gap-3 border-t border-border/60 pt-4"><span className="text-xs text-muted-foreground">已识别 {drafts.length} 条 · 拒绝的草稿不会入账</span><Button type="button" onClick={() => void confirm()} disabled={confirming || capture?.status !== "review"} className="gap-2"><Check className="size-4" />{confirming ? "入账中" : "确认并入账"}</Button></div></div> : <div className="flex min-h-64 flex-col items-center justify-center rounded-md border border-dashed border-border/60 bg-muted/15 px-6 text-center"><Camera className="size-8 text-muted-foreground/50" /><p className="mt-3 text-sm font-medium">等待识别结果</p><p className="mt-1 max-w-sm text-xs leading-5 text-muted-foreground">识别后，所有结果都会先停留在这里，你可以逐条检查再提交。</p></div>}</section>
    </div>
  </WorkspaceShell>
}

function DraftRow({ item, accounts, categories, onUpdate, onReject, onRestore }: { item: Draft; accounts: FinanceAccount[]; categories: FinanceCategory[]; onUpdate: (id: number, patch: Partial<Draft>) => void; onReject: (id: number) => void; onRestore: (id: number) => void }) {
  const rejected = item.status === "rejected"
  const availableCategories = categories.filter((category) => category.direction === "both" || category.direction === item.transactionType)
  return <div className={`rounded-md border p-3 ${rejected ? "border-border/50 bg-muted/30 opacity-60" : "border-border/70"}`}><div className="flex items-start justify-between gap-3"><div className="flex min-w-0 items-center gap-2"><span className={`rounded px-1.5 py-0.5 text-[11px] ${item.transactionType === "income" ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-300" : "bg-rose-50 text-rose-700 dark:bg-rose-950/30 dark:text-rose-300"}`}>{item.transactionType === "income" ? "收入" : "支出"}</span><span className="truncate text-sm font-medium">{item.merchant || item.categoryHint || "未命名账目"}</span>{item.confidence != null ? <span className="text-[11px] text-muted-foreground">置信度 {(item.confidence * 100).toFixed(0)}%</span> : null}</div>{rejected ? <Button type="button" variant="ghost" size="sm" onClick={() => onRestore(item.id)}>恢复</Button> : <Button type="button" variant="ghost" size="icon-sm" title="拒绝此草稿" onClick={() => onReject(item.id)}><Trash2 className="size-4" /></Button>}</div>{!rejected ? <div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-3"><label className="space-y-1 text-[11px] font-medium">金额<Input type="number" min="0.01" step="0.01" value={item.amount ?? ""} onChange={(event) => onUpdate(item.id, { amount: event.target.value ? Number(event.target.value) : null })} /></label><label className="space-y-1 text-[11px] font-medium">日期<Input type="datetime-local" value={item.occurredAt ? item.occurredAt.slice(0, 16) : ""} onChange={(event) => onUpdate(item.id, { occurredAt: event.target.value ? `${event.target.value}:00+08:00` : null })} /></label><label className="space-y-1 text-[11px] font-medium">账户<select required value={item.accountId ?? ""} onChange={(event) => onUpdate(item.id, { accountId: event.target.value ? Number(event.target.value) : null })} className="h-9 w-full rounded-md border border-input bg-background px-2 text-sm"><option value="">选择账户</option>{accounts.filter((account) => !account.archived).map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}</select></label><label className="space-y-1 text-[11px] font-medium">分类<select required value={item.categoryId ?? ""} onChange={(event) => onUpdate(item.id, { categoryId: event.target.value ? Number(event.target.value) : null })} className="h-9 w-full rounded-md border border-input bg-background px-2 text-sm"><option value="">选择分类</option>{availableCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label><label className="space-y-1 text-[11px] font-medium">商户 / 来源<Input value={item.merchant ?? ""} onChange={(event) => onUpdate(item.id, { merchant: event.target.value })} placeholder="可选" /></label><label className="space-y-1 text-[11px] font-medium">备注<Input value={item.note ?? ""} onChange={(event) => onUpdate(item.id, { note: event.target.value })} placeholder="可选" /></label></div> : <div className="mt-2 text-xs text-muted-foreground">{formatMoney(item.amount)} · {item.merchant || "未命名"} · 已标记为拒绝</div>}</div>
}
