import { Check, Pencil, Wallet, X } from "lucide-react"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { notification } from "@/components/ui/notification"
import { updatePortfolioAccount } from "@/services/market/portfolio/portfolio"
import type { PortfolioOverview } from "@/services/market/portfolio/types"

interface PortfolioAssetSnapshotProps {
  overview: PortfolioOverview
  onSaved: () => void
}

const money = (value: number | null | undefined) => value == null ? "--" : Number(value).toFixed(2)

function signedMoney(value: number | null | undefined) {
  if (value == null) return "--"
  const amount = Number(value)
  return `${amount > 0 ? "+" : ""}${amount.toFixed(2)}`
}

function pnlTone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

export function PortfolioAssetSnapshot({ overview, onSaved }: PortfolioAssetSnapshotProps) {
  const [editingCash, setEditingCash] = useState(false)
  const [cashDraft, setCashDraft] = useState("")
  const [saving, setSaving] = useState(false)
  const marketValue = Number(overview.marketValue ?? 0)
  const availableCash = Number(overview.availableCash ?? 0)
  const totalAssets = Number(overview.totalAssets ?? marketValue + availableCash)
  const cashRatio = totalAssets > 0 ? (availableCash / totalAssets) * 100 : 0
  const holdingsRatio = Math.max(0, 100 - cashRatio)

  function startEditing() {
    setCashDraft(availableCash.toFixed(2))
    setEditingCash(true)
  }

  function cancelEditing() {
    if (saving) return
    setEditingCash(false)
    setCashDraft("")
  }

  async function saveCash() {
    const value = Number(cashDraft)
    if (!Number.isFinite(value) || value < 0) {
      notification.warn({ title: "可用资金格式不正确", description: "请输入大于或等于 0 的金额。" })
      return
    }
    setSaving(true)
    try {
      await updatePortfolioAccount(value)
      notification.success({ title: "可用资金已更新", description: "总资产和资金结构已重新计算。" })
      setEditingCash(false)
      setCashDraft("")
      onSaved()
    } catch (cause) {
      notification.danger({ title: "可用资金更新失败", description: cause instanceof Error ? cause.message : String(cause) })
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="portfolio-snapshot-heading">
      <h2 id="portfolio-snapshot-heading" className="sr-only">资产快照</h2>
      <div className="grid lg:grid-cols-[minmax(250px,1.05fr)_minmax(0,1.95fr)]">
        <div className="border-b border-border/60 bg-muted/25 px-5 py-5 lg:border-b-0 lg:border-r">
          <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground"><Wallet className="size-4" aria-hidden="true" />资产总览</div>
          <p className="mt-2 font-mono text-3xl font-semibold tracking-tight tabular-nums">{money(totalAssets)}</p>
          <p className="mt-1 text-xs text-muted-foreground">持仓市值 + 可用资金</p>
          <div className="mt-5 flex items-center justify-between gap-3 border-t border-border/60 pt-3"><span className="text-xs text-muted-foreground">今日收益</span><span className={`font-mono text-sm font-semibold tabular-nums ${pnlTone(overview.todayPnl)}`}>{signedMoney(overview.todayPnl)}</span></div>
        </div>
        <div className="grid sm:grid-cols-2">
          <AssetValue label="当前持仓市值" value={money(marketValue)} detail="按最新可用行情估值" />
          <div className="border-b border-border/60 px-4 py-4 sm:border-l">
            <div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="text-xs text-muted-foreground">可用资金</p>{editingCash ? <div className="mt-2 flex items-center gap-2"><Input autoFocus inputMode="decimal" value={cashDraft} onChange={(event) => setCashDraft(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") void saveCash(); if (event.key === "Escape") cancelEditing() }} className="h-8 min-w-0 font-mono tabular-nums" aria-label="可用资金" /><Button size="icon-sm" onClick={() => void saveCash()} disabled={saving} aria-label="保存可用资金" title="保存"><Check className="size-4" /></Button><Button size="icon-sm" variant="ghost" onClick={cancelEditing} disabled={saving} aria-label="取消编辑" title="取消"><X className="size-4" /></Button></div> : <p className="mt-1.5 font-mono text-lg font-semibold tabular-nums">{money(availableCash)}</p>}</div>{!editingCash ? <Button size="icon-sm" variant="ghost" onClick={startEditing} aria-label="编辑可用资金" title="编辑可用资金"><Pencil className="size-4" /></Button> : null}</div>
            <p className="mt-1 text-[11px] text-muted-foreground">手动维护，不从交易截图推断</p>
          </div>
          <div className="border-t border-border/60 px-4 py-4 sm:col-span-2">
            <div className="flex items-center justify-between gap-3 text-xs"><span className="text-muted-foreground">资金结构</span><span className="font-mono tabular-nums text-muted-foreground">持仓 {holdingsRatio.toFixed(1)}% · 现金 {cashRatio.toFixed(1)}%</span></div>
            <div className="mt-2 flex h-2 overflow-hidden rounded-full bg-muted" aria-label={`持仓 ${holdingsRatio.toFixed(1)}%、现金 ${cashRatio.toFixed(1)}%`}><span className="bg-primary" style={{ width: `${Math.max(0, Math.min(100, holdingsRatio))}%` }} /><span className="bg-muted-foreground/40" style={{ width: `${Math.max(0, Math.min(100, cashRatio))}%` }} /></div>
          </div>
        </div>
      </div>
    </section>
  )
}

function AssetValue({ label, value, detail }: { label: string; value: string; detail: string }) {
  return <div className="border-b border-border/60 px-4 py-4 sm:border-b-0"><p className="text-xs text-muted-foreground">{label}</p><p className="mt-1.5 font-mono text-lg font-semibold tabular-nums">{value}</p><p className="mt-1 text-[11px] text-muted-foreground">{detail}</p></div>
}
