import { ArrowDownLeft, ArrowLeftRight, ArrowUpRight, Check, Plus } from "lucide-react"
import { useState, type FormEvent } from "react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import type { FinanceAccount, FinanceCategory } from "@/services/finance/ledger"
import { today } from "../lib/format"

type EntryType = "expense" | "income" | "transfer"

export function QuickEntryForm({ accounts, categories, onSaved }: { accounts: FinanceAccount[]; categories: FinanceCategory[]; onSaved: (payload: Record<string, unknown>) => Promise<void> }) {
  const [type, setType] = useState<EntryType>("expense")
  const [amount, setAmount] = useState("")
  const [accountId, setAccountId] = useState("")
  const [toAccountId, setToAccountId] = useState("")
  const [categoryId, setCategoryId] = useState("")
  const [occurredAt, setOccurredAt] = useState(today())
  const [merchant, setMerchant] = useState("")
  const [note, setNote] = useState("")
  const [saving, setSaving] = useState(false)

  const activeCategories = categories.filter((item) => item.direction === type || item.direction === "both")
  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setSaving(true)
    try {
      await onSaved({ transactionType: type, amount: Number(amount), accountId: Number(accountId), toAccountId: type === "transfer" ? Number(toAccountId) : undefined, categoryId: type === "transfer" ? undefined : Number(categoryId), occurredAt: `${occurredAt}T12:00:00+08:00`, merchant, note })
      setAmount(""); setMerchant(""); setNote("")
    } finally { setSaving(false) }
  }

  const types = [{ value: "expense" as const, label: "支出", icon: ArrowUpRight }, { value: "income" as const, label: "收入", icon: ArrowDownLeft }, { value: "transfer" as const, label: "转账", icon: ArrowLeftRight }]
  return (
    <section className="min-w-0 rounded-lg border border-border/70 bg-card p-4">
      <div className="mb-4 flex items-start justify-between gap-3"><div><h2 className="text-base font-semibold">快速记一笔</h2><p className="mt-1 text-xs text-muted-foreground">先记下事实，后续可在账本中归档或修正。</p></div><Plus className="size-4 text-muted-foreground" /></div>
      <form onSubmit={submit} className="space-y-3">
        <div className="grid grid-cols-3 gap-1 rounded-md bg-muted/60 p-1">{types.map(({ value, label, icon: Icon }) => <button key={value} type="button" onClick={() => setType(value)} className={`flex items-center justify-center gap-1.5 rounded px-2 py-2 text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${type === value ? "bg-background font-medium shadow-sm" : "text-muted-foreground hover:text-foreground"}`} aria-pressed={type === value}><Icon className="size-3.5" />{label}</button>)}</div>
        <div className="grid gap-3 sm:grid-cols-2">
          <label className="space-y-1.5 text-xs font-medium"><span>金额</span><Input required min="0.01" step="0.01" type="number" inputMode="decimal" placeholder="0.00" value={amount} onChange={(event) => setAmount(event.target.value)} /></label>
          <label className="space-y-1.5 text-xs font-medium"><span>日期</span><Input required type="date" value={occurredAt} onChange={(event) => setOccurredAt(event.target.value)} /></label>
          <label className="space-y-1.5 text-xs font-medium"><span>{type === "transfer" ? "转出账户" : "资金账户"}</span><select required value={accountId} onChange={(event) => setAccountId(event.target.value)} className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"><option value="">选择账户</option>{accounts.filter((item) => !item.archived).map((item) => <option key={item.id} value={item.id}>{item.name} · {item.nature === "liability" ? "负债" : "资产"}</option>)}</select></label>
          {type === "transfer" ? <label className="space-y-1.5 text-xs font-medium"><span>转入账户</span><select required value={toAccountId} onChange={(event) => setToAccountId(event.target.value)} className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"><option value="">选择账户</option>{accounts.filter((item) => !item.archived && String(item.id) !== accountId).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label> : <label className="space-y-1.5 text-xs font-medium"><span>分类</span><select required value={categoryId} onChange={(event) => setCategoryId(event.target.value)} className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"><option value="">选择分类</option>{activeCategories.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>}
          <label className="space-y-1.5 text-xs font-medium"><span>商户 / 来源</span><Input placeholder="例如：便利店" value={merchant} onChange={(event) => setMerchant(event.target.value)} /></label>
          <label className="space-y-1.5 text-xs font-medium"><span>备注</span><Input placeholder="可选" value={note} onChange={(event) => setNote(event.target.value)} /></label>
        </div>
        <div className="flex justify-end"><Button type="submit" disabled={saving || !accounts.length} className="gap-2">{saving ? "保存中" : <><Check className="size-4" />入账</>}</Button></div>
      </form>
    </section>
  )
}
