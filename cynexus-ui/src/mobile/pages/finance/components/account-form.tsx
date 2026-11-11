import { useRef, useState, type FormEvent } from "react"
import { Button } from "@heroui/react/button"
import { Input } from "@heroui/react/input"
import { InputGroup } from "@heroui/react/input-group"
import { Label } from "@heroui/react/label"
import { ListBox } from "@heroui/react/list-box"
import { Modal } from "@heroui/react/modal"
import { Select } from "@heroui/react/select"
import { TextArea } from "@heroui/react/textarea"
import { TextField } from "@heroui/react/textfield"
import { ArrowLeftRight, Check, ChevronDown, CreditCard, FileText, Landmark, LockKeyhole, TrendingUp, Wallet, X } from "lucide-react"

import { useMobilePortal } from "@/mobile/hooks/use-mobile-portal"
import { saveFinanceAccount, type FinanceAccount } from "@/services/finance/ledger"
import "./account-form.css"

type AccountType = FinanceAccount["accountType"]
type Account = { name: string; type: AccountType; currency: string; opening: string; balance: number; notes: string; limit: string; billing: string; due: string }
const types: { id: AccountType; name: string }[] = [
  { id: "bank", name: "Bank account" }, { id: "cash", name: "Cash" }, { id: "e_wallet", name: "E-wallet" },
  { id: "credit_card", name: "Credit card" }, { id: "huabei", name: "Huabei" }, { id: "baitiao", name: "Baitiao" },
  { id: "douyin_monthly", name: "Douyin monthly" }, { id: "investment", name: "Investment" }, { id: "other", name: "Other account" },
]
const currencies = ["CNY", "USD", "EUR", "GBP", "JPY", "HKD"]
const symbols: Record<string, string> = { CNY: "¥", USD: "$", EUR: "€", GBP: "£", JPY: "¥", HKD: "HK$" }
const creditTypes = new Set<AccountType>(["credit_card", "huabei", "baitiao", "douyin_monthly"])
const money = (value: number, currency: string) => `${symbols[currency] ?? currency + " "}${value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const validAmount = (value: string) => /^\d+(?:\.\d{1,2})?$/.test(value.trim()) && Number.isFinite(Number(value)) && Number(value) <= 999999999.99

export function AccountFormSheet({ mode, account: source, onClose, onSaved }: { mode: "edit" | "add"; account?: FinanceAccount; onClose: () => void; onSaved: (account: FinanceAccount) => Promise<void> | void }) {
  const portal = useMobilePortal()
  const account: Account = { name: source?.name ?? "", type: source?.accountType ?? "bank", currency: source?.currency ?? "CNY", opening: (source?.openingBalance ?? 0).toFixed(2), balance: source?.balance ?? 0, notes: source?.notes ?? "", limit: String(source?.creditLimit ?? ""), billing: String(source?.billingDay ?? ""), due: String(source?.dueDay ?? "") }
  const [type, setType] = useState<AccountType>(account.type)
  const [name, setName] = useState(account.name)
  const [currency, setCurrency] = useState(account.currency)
  const [opening, setOpening] = useState(account.opening)
  const [limit, setLimit] = useState(account.limit)
  const [billing, setBilling] = useState(account.billing)
  const [due, setDue] = useState(account.due)
  const [notes, setNotes] = useState(account.notes)
  const [notesOpen, setNotesOpen] = useState(Boolean(account.notes))
  const [error, setError] = useState<{ field: string; message: string } | null>(null)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [typeOpen, setTypeOpen] = useState(false)
  const [currencyOpen, setCurrencyOpen] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)
  const savingRef = useRef(false)
  const savedRef = useRef(false)
  const errorRef = useRef<HTMLParagraphElement>(null)
  const credit = creditTypes.has(type)
  const isEdit = mode === "edit"
  const TypeIcon = credit ? CreditCard : type === "investment" ? TrendingUp : type === "bank" ? Landmark : Wallet

  function updateType(value: string) { setType(value as AccountType); setError(null) }
  function fail(field: string, message: string) {
    setError({ field, message })
    requestAnimationFrame(() => { errorRef.current?.focus(); scrollRef.current?.querySelector<HTMLInputElement>(`[name="${field}"]`)?.focus() })
  }
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (savingRef.current || savedRef.current) return
    if (!name.trim()) return fail("name", "Enter an account name.")
    if (!validAmount(opening)) return fail("opening", "Enter a balance of 0 or more, with up to 2 decimals.")
    if (credit && limit.trim() && !validAmount(limit)) return fail("limit", "Enter a valid credit limit, or leave it blank.")
    for (const [field, value] of [["billing", billing], ["due", due]]) {
      if (credit && value.trim() && (!/^\d+$/.test(value) || Number(value) < 1 || Number(value) > 31)) return fail(field, "Choose a day from 1 to 31, or leave it blank.")
    }
    savingRef.current = true; setSaving(true); setError(null)
    let saved: FinanceAccount
    try {
      saved = await saveFinanceAccount({
        ...(source ? { id: source.id } : {}), name: name.trim(), accountType: type,
        nature: credit ? "credit" : "asset", currency: source?.currency ?? currency,
        openingBalance: Number(opening), creditLimit: credit && limit.trim() ? Number(limit) : null,
        billingDay: credit && billing.trim() ? Number(billing) : null,
        dueDay: credit && due.trim() ? Number(due) : null,
        color: source?.color ?? null, notes: notes.trim() || null,
      })
    } catch (cause) {
      savingRef.current = false; setSaving(false)
      return fail("", cause instanceof Error ? cause.message : "Could not save this account. Please try again.")
    }
    savedRef.current = true
    setSaved(true)
    try { await onSaved(saved); onClose() }
    catch { savingRef.current = false; setSaving(false); fail("", "Account saved. Close this sheet and refresh to update your accounts.") }

  }

  const content = <form className="fac-account-form" onSubmit={(event) => void submit(event)} noValidate aria-busy={saving}>
    <div className="fac-sheet-grip" aria-hidden="true" />
    <header className="fac-sheet-heading"><h1>{isEdit ? "Edit account" : "Add account"}</h1><Button isIconOnly variant="ghost" className="fac-sheet-close" aria-label="Close account sheet" isDisabled={saving} onPress={onClose}><X /></Button></header>
    <div className="fac-sheet-scroll" ref={scrollRef}>
      <section className="fac-opening-section" aria-label="Starting balance">
        <TextField fullWidth className="fac-account-field fac-opening-field" name="opening" value={opening} onChange={(value) => { setOpening(value); setError(null) }} isDisabled={saving || saved} isInvalid={error?.field === "opening"}>
          <Label>{credit ? "Opening debt" : "Opening balance"}<span>{currency}</span></Label>
          <InputGroup fullWidth><InputGroup.Prefix>{symbols[currency]}</InputGroup.Prefix><InputGroup.Input inputMode="decimal" autoComplete="off" placeholder="0.00" /></InputGroup>
        </TextField>
        <p className="fac-balance-hint">{credit ? "Amount owed before your first recorded transaction." : "Starting amount before your recorded activity."}</p>
        {isEdit && <div className="fac-current-balance"><span><LockKeyhole />{credit ? "Current debt" : "Current balance"}</span><strong>{source?.balance == null ? "Unavailable" : money(source.balance, currency)}</strong></div>}
      </section>
      <TextField fullWidth className="fac-account-field fac-name-field" name="name" value={name} onChange={(value) => { setName(value); setError(null) }} isDisabled={saving || saved} isInvalid={error?.field === "name"}>
        <Label><Wallet />Account name</Label><Input placeholder="Add a name" maxLength={80} autoComplete="off" />
      </TextField>
      <div className="fac-account-type-row">
        <Select fullWidth className="fac-account-type-select" name="type" value={type} onChange={(value) => updateType(String(value))} isOpen={typeOpen} onOpenChange={setTypeOpen} isDisabled={saving || saved}>
          <Label><TypeIcon />Account type</Label><Select.Trigger><Select.Value /><Select.Indicator /></Select.Trigger>
          <Select.Popover className="fac-account-choice-popover" placement="bottom start" shouldFlip>
            <ListBox aria-label="Account type">{types.map((option) => <ListBox.Item key={option.id} id={option.id} textValue={option.name}>{option.name}<ListBox.ItemIndicator /></ListBox.Item>)}</ListBox>
          </Select.Popover>
        </Select>
        {isEdit ? <div className="fac-currency-readonly"><span className="fac-field-label" id="currency-label"><Landmark />Currency</span><div aria-labelledby="currency-label"><span>{currency}</span><LockKeyhole aria-label="Currency cannot be changed" /></div></div> :
          <Select fullWidth className="fac-currency-select" name="currency" value={currency} onChange={(value) => { setCurrency(String(value)); setError(null) }} isOpen={currencyOpen} onOpenChange={setCurrencyOpen} isDisabled={saving || saved}>
            <Label><Landmark />Currency</Label><Select.Trigger><Select.Value /><Select.Indicator /></Select.Trigger>
            <Select.Popover className="fac-account-choice-popover fac-currency-popover"><ListBox aria-label="Currency">{currencies.map((value) => <ListBox.Item key={value} id={value} textValue={value}>{value}<ListBox.ItemIndicator /></ListBox.Item>)}</ListBox></Select.Popover>
          </Select>}
      </div>
      <div className="fac-account-kind" aria-live="polite"><span>{credit ? "Credit account" : "Asset account"}</span>{type === "investment" && <span className="fac-transfer-only"><ArrowLeftRight />Transfers only</span>}</div>
      {credit && <section className="fac-credit-section" aria-label="Credit details">
        <TextField fullWidth className="fac-account-field fac-credit-limit-row" name="limit" value={limit} onChange={(value) => { setLimit(value); setError(null) }} isDisabled={saving || saved} isInvalid={error?.field === "limit"}>
          <Label><CreditCard />Credit limit</Label><Input inputMode="decimal" placeholder="Optional" autoComplete="off" />
        </TextField>
        <div className="fac-credit-days">
          <TextField fullWidth className="fac-account-field" name="billing" value={billing} onChange={(value) => { setBilling(value); setError(null) }} isDisabled={saving || saved} isInvalid={error?.field === "billing"}><Label>Billing day</Label><InputGroup fullWidth><InputGroup.Input inputMode="numeric" maxLength={2} placeholder="Not set" /><InputGroup.Suffix>of month</InputGroup.Suffix></InputGroup></TextField>
          <TextField fullWidth className="fac-account-field" name="due" value={due} onChange={(value) => { setDue(value); setError(null) }} isDisabled={saving || saved} isInvalid={error?.field === "due"}><Label>Due day</Label><InputGroup fullWidth><InputGroup.Input inputMode="numeric" maxLength={2} placeholder="Not set" /><InputGroup.Suffix>of month</InputGroup.Suffix></InputGroup></TextField>
        </div>
        <p className="fac-balance-hint">Days 1–31 · leave blank if not applicable.</p>
      </section>}
      <section className="fac-notes-section">
        <Button type="button" variant="ghost" className="fac-notes-toggle" aria-expanded={notesOpen} aria-controls="account-notes" isDisabled={saving || saved} onPress={() => setNotesOpen((current) => !current)}><FileText /><span>{notesOpen || notes ? "Notes" : "Add a note"}</span><small>{notes ? `${notes.length}/200` : "Optional"}</small><ChevronDown className={notesOpen ? "fac-is-open" : ""} /></Button>
        {notesOpen && <TextField fullWidth name="notes" value={notes} onChange={setNotes} isDisabled={saving || saved}><Label className="fac-sr-only">Notes</Label><TextArea id="account-notes" rows={2} maxLength={200} placeholder="What is this account for?" /></TextField>}
      </section>
    </div>
    <footer className="fac-sheet-footer">
      {error && <p className="fac-form-error" ref={errorRef} tabIndex={-1} role="alert">{error.message}</p>}
      <div className="fac-footer-buttons"><Button type="submit" variant="primary" isPending={saving} isDisabled={saving || saved}>{saving ? "Saving…" : <><Check />{isEdit ? "Save changes" : "Add account"}</>}</Button></div>
      <p>Current {credit ? "debt" : "balance"} follows your saved activity.</p>
    </footer>
  </form>
  return <Modal.Backdrop UNSTABLE_portalContainer={portal} className="fac-account-backdrop" isOpen onOpenChange={(open) => { if (!open && !savingRef.current) onClose() }} isDismissable={!saving} isKeyboardDismissDisabled={saving} variant="opaque">
    <Modal.Container placement="bottom" scroll="inside" className="fac-account-modal-container">
      <Modal.Dialog className="fac-account-sheet" aria-label={isEdit ? "Edit account" : "Add account"}>{content}</Modal.Dialog>
    </Modal.Container>
  </Modal.Backdrop>
}

