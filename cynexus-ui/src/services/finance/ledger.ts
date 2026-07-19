import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const FINANCE_API = `${SERVICE_PREFIX.core}/finance`

export interface FinanceAccount {
  id: number
  name: string
  accountType: "cash" | "bank" | "e_wallet" | "huabei" | "baitiao" | "douyin_monthly" | "credit_card" | "investment" | "other"
  nature: "asset" | "credit"
  currency: string
  openingBalance: number | null
  creditLimit: number | null
  billingDay: number | null
  dueDay: number | null
  color: string | null
  notes: string | null
  archived: boolean
  balance: number | null
  createdAt?: string | null
}

export interface FinanceInstallment {
  id: number
  planId: number
  installmentNo: number
  dueDate: string
  principalAmount: number
  feeAmount: number
  amount: number
  status: "pending" | "paid" | "cancelled"
  paidAt: string | null
  paymentTransactionId: number | null
}

export interface FinanceInstallmentPlan {
  id: number
  accountId: number
  accountName: string | null
  accountType: FinanceAccount["accountType"]
  purchaseTransactionId: number | null
  merchant: string | null
  totalAmount: number
  feeTotal: number
  installmentCount: number
  firstDueDate: string
  intervalMonths: number
  status: "active" | "completed" | "cancelled"
  note: string | null
  createdAt?: string | null
  updatedAt?: string | null
  installments: FinanceInstallment[]
}

export interface FinanceCategory {
  id: number
  parentId: number | null
  name: string
  direction: "expense" | "income" | "both"
  icon: string | null
  color: string | null
  sortOrder: number
  system: boolean
  archived: boolean
}

export interface FinanceTransaction {
  id: number
  transactionType: "expense" | "income" | "transfer"
  accountId: number
  accountName: string | null
  toAccountId: number | null
  toAccountName: string | null
  categoryId: number | null
  categoryName: string | null
  amount: number
  currency: string
  occurredAt: string
  merchant: string | null
  note: string | null
  source: string
  archived: boolean
  attachmentCount: number
  createdAt?: string | null
}

export interface FinanceOverview {
  startDate: string
  endDate: string
  income: number
  expense: number
  net: number
  assetTotal: number
  liabilityTotal: number
  netAssets: number
  accounts: FinanceAccount[]
  categories: Array<{ categoryId: number; categoryName: string; direction: "expense" | "income"; amount: number; percentage: number }>
  daily: Array<{ date: string; income: number; expense: number }>
  recentTransactions: FinanceTransaction[]
}

export interface FinanceAiCaptureItem {
  id: number
  imageId: number | null
  itemIndex: number
  transactionType: "expense" | "income"
  occurredAt: string | null
  amount: number | null
  currency: string
  merchant: string | null
  categoryHint: string | null
  categoryId: number | null
  categoryName: string | null
  accountId: number | null
  accountName: string | null
  installmentCount: number | null
  installmentFee: number | null
  note: string | null
  confidence: number | null
  rawText: string | null
  status: "draft" | "confirmed" | "rejected"
  transactionId: number | null
}

export interface FinanceAiCapture {
  id: number
  status: "uploaded" | "processing" | "review" | "confirmed" | "failed" | "archived"
  sourceCount: number
  modelCode: string | null
  errorMessage: string | null
  processingMode?: "auto" | "review"
  autoPostedCount?: number
  reviewCount?: number
  createdAt: string
  updatedAt: string
  items: FinanceAiCaptureItem[]
}

async function data<T>(response: Response, fallback: string): Promise<T> {
  try {
    return await cynexusResult<T>(response)
  } catch (error) {
    throw new Error(error instanceof Error ? error.message : fallback, { cause: error })
  }
}

function query(params: Record<string, string | number | undefined>) {
  const value = new URLSearchParams()
  Object.entries(params).forEach(([key, item]) => { if (item !== undefined) value.set(key, String(item)) })
  return value.toString()
}

export async function fetchFinanceOverview(params: { start?: string; end?: string } = {}) {
  return data<FinanceOverview>(await fetchWithRetry(`${FINANCE_API}/overview?${query(params)}`, { cache: "no-store" }), "账本统计加载失败")
}

export async function fetchFinanceAccounts(includeArchived = false) {
  return data<FinanceAccount[]>(await fetchWithRetry(`${FINANCE_API}/accounts?includeArchived=${includeArchived}`, { cache: "no-store" }), "账户加载失败")
}

export async function saveFinanceAccount(payload: Partial<FinanceAccount>) {
  return data<FinanceAccount>(await fetchWithRetry(`${FINANCE_API}/accounts`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) }), "账户保存失败")
}

export async function archiveFinanceAccount(id: number) {
  return data<boolean>(await fetchWithRetry(`${FINANCE_API}/accounts/${id}`, { method: "DELETE" }), "账户归档失败")
}

export async function restoreFinanceAccount(id: number) {
  return data<boolean>(await fetchWithRetry(`${FINANCE_API}/accounts/${id}/restore`, { method: "POST" }), "账户恢复失败")
}

export async function fetchFinanceInstallmentPlans(accountId?: number) {
  return data<FinanceInstallmentPlan[]>(await fetchWithRetry(`${FINANCE_API}/installments/plans${accountId ? `?accountId=${accountId}` : ""}`, { cache: "no-store" }), "分期计划加载失败")
}

export async function saveFinanceInstallmentPlan(payload: { accountId: number; purchaseTransactionId?: number; merchant?: string; totalAmount: number; feeTotal?: number; installmentCount: number; firstDueDate: string; intervalMonths?: number; note?: string }) {
  return data<FinanceInstallmentPlan>(await fetchWithRetry(`${FINANCE_API}/installments/plans`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) }), "分期计划保存失败")
}

export async function payFinanceInstallment(planId: number, installmentNo: number, payload: { paymentAccountId: number; occurredAt?: string; note?: string }) {
  return data<FinanceInstallmentPlan>(await fetchWithRetry(`${FINANCE_API}/installments/plans/${planId}/installments/${installmentNo}/pay`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) }), "分期还款失败")
}

export async function fetchFinanceCategories(includeArchived = false) {
  return data<FinanceCategory[]>(await fetchWithRetry(`${FINANCE_API}/categories?includeArchived=${includeArchived}`, { cache: "no-store" }), "分类加载失败")
}

export async function saveFinanceCategory(payload: Partial<FinanceCategory>) {
  return data<FinanceCategory>(await fetchWithRetry(`${FINANCE_API}/categories`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) }), "分类保存失败")
}

export async function fetchFinanceTransactions(params: { start?: string; end?: string; limit?: number } = {}) {
  return data<FinanceTransaction[]>(await fetchWithRetry(`${FINANCE_API}/transactions?${query(params)}`, { cache: "no-store" }), "账目加载失败")
}

export async function saveFinanceTransaction(payload: Partial<FinanceTransaction>) {
  return data<FinanceTransaction>(await fetchWithRetry(`${FINANCE_API}/transactions`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) }), "账目保存失败")
}

export async function archiveFinanceTransaction(id: number) {
  return data<boolean>(await fetchWithRetry(`${FINANCE_API}/transactions/${id}`, { method: "DELETE" }), "账目归档失败")
}

export async function createFinanceAiCapture(files: File[], options: { review?: boolean; idempotencyKey?: string } = {}) {
  const body = new FormData()
  files.forEach((file) => body.append("image", file))
  const headers = new Headers()
  if (options.idempotencyKey) headers.set("Idempotency-Key", options.idempotencyKey)
  const endpoint = options.review ? `${FINANCE_API}/ai-captures/review` : `${FINANCE_API}/ai-captures`
  return data<FinanceAiCapture>(await fetchWithRetry(endpoint, { method: "POST", headers, body }), "图片解析失败")
}

export async function confirmFinanceAiCapture(id: number, items: Array<Partial<FinanceAiCaptureItem>>) {
  return data<FinanceAiCapture>(await fetchWithRetry(`${FINANCE_API}/ai-captures/${id}/confirm`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ items }),
  }), "AI 草稿入账失败")
}

export async function archiveFinanceAiCapture(id: number) {
  return data<boolean>(await fetchWithRetry(`${FINANCE_API}/ai-captures/${id}`, { method: "DELETE" }), "AI 批次归档失败")
}
