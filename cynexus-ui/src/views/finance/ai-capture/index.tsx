import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  ArrowDownLeft,
  ArrowUpRight,
  Archive,
  Camera,
  Check,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  FileImage,
  FileClock,
  LoaderCircle,
  RefreshCw,
  ReceiptText,
  Sparkles,
  Trash2,
  X,
} from "lucide-react";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { Input } from "@/components/ui/input";
import { notification } from "@/components/ui/notification";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useIsMobile } from "@/hooks/use-mobile";
import { WorkspaceShell } from "@/layout/workspace-shell";
import { AiBindingProviderSelect } from "@/components/ai/ai-binding-provider-select";
import {
  archiveFinanceAiCapture,
  fetchFinanceAccounts,
  fetchFinanceAiCaptureHistory,
  fetchFinanceCategories,
  createFinanceAiCapture,
  confirmFinanceAiCapture,
  retryFinanceAiCapture,
  type FinanceAccount,
  type FinanceAiCapture,
  type FinanceAiCaptureItem,
  type FinanceCategory,
} from "@/services/finance/ledger";
import { FinancePageHeader } from "../components/page-header";
import { formatMoney } from "../lib/format";

type Draft = FinanceAiCaptureItem;

function editable(item: Draft, patch: Partial<Draft>) {
  return { ...item, ...patch };
}

const statusLabels: Record<FinanceAiCapture["status"], string> = {
  uploaded: "已上传",
  processing: "识别中",
  review: "待审核",
  confirmed: "已入账",
  failed: "识别失败",
  archived: "已归档",
};

function statusClass(status: FinanceAiCapture["status"]) {
  if (status === "confirmed") return "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300";
  if (status === "review") return "bg-amber-500/10 text-amber-700 dark:text-amber-300";
  if (status === "failed") return "bg-rose-500/10 text-rose-700 dark:text-rose-300";
  if (status === "processing") return "bg-sky-500/10 text-sky-700 dark:text-sky-300";
  return "bg-muted text-muted-foreground";
}

function capturePayload(items: Draft[]) {
  return items.map((item) => ({
    id: item.id,
    transactionType: item.transactionType,
    occurredAt: item.occurredAt,
    amount: item.amount,
    currency: item.currency,
    merchant: item.merchant,
    categoryId: item.categoryId,
    accountId: item.accountId,
    installmentCount: item.installmentCount,
    installmentFee: item.installmentFee,
    note: item.note,
    status: item.status,
  }));
}

export default function FinanceAiCapturePage() {
  const isMobile = useIsMobile();
  const [files, setFiles] = useState<File[]>([]);
  const [capture, setCapture] = useState<FinanceAiCapture | null>(null);
  const [drafts, setDrafts] = useState<Draft[]>([]);
  const [accounts, setAccounts] = useState<FinanceAccount[]>([]);
  const [categories, setCategories] = useState<FinanceCategory[]>([]);
  const [uploading, setUploading] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [history, setHistory] = useState<FinanceAiCapture[]>([]);
  const [historyTotal, setHistoryTotal] = useState(0);
  const [historyPage, setHistoryPage] = useState(1);
  const [historyPageSize, setHistoryPageSize] = useState(10);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [historySelected, setHistorySelected] = useState<FinanceAiCapture | null>(null);
  const [historyDrafts, setHistoryDrafts] = useState<Draft[]>([]);
  const [retrying, setRetrying] = useState(false);
  const [archiving, setArchiving] = useState(false);

  const loadHistory = useCallback(async () => {
    setHistoryLoading(true);
    try {
      const result = await fetchFinanceAiCaptureHistory({ limit: historyPageSize, offset: (historyPage - 1) * historyPageSize, status: "all" });
      if (!result.items.length && result.total > 0 && historyPage > 1) {
        setHistoryPage((current) => current - 1);
        return;
      }
      setHistory(result.items);
      setHistoryTotal(result.total);
    } catch (cause) {
      notification.danger({
        title: "AI 入账历史加载失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setHistoryLoading(false);
    }
  }, [historyPage, historyPageSize]);

  useEffect(() => {
    void Promise.all([fetchFinanceAccounts(), fetchFinanceCategories()])
      .then(([nextAccounts, nextCategories]) => {
        setAccounts(nextAccounts);
        setCategories(nextCategories);
      })
      .catch((cause) =>
        notification.danger({
          title: "图片记账准备失败",
          description: cause instanceof Error ? cause.message : String(cause),
        }),
      );
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadHistory(), 0);
    return () => window.clearTimeout(timer);
  }, [loadHistory]);

  const previews = useMemo(
    () => files.map((file) => ({ file, url: URL.createObjectURL(file) })),
    [files],
  );
  useEffect(
    () => () => previews.forEach((item) => URL.revokeObjectURL(item.url)),
    [previews],
  );

  const chooseFiles = useCallback((list: FileList | null) => {
    if (!list) return;
    const selected = Array.from(list);
    if (selected.length !== 1) {
      notification.warn({
        title: "请只选择一张图片",
        description: "快捷记账一次解析一张小票或账单图片。",
      });
      return;
    }
    const invalid = selected.find(
      (file) => !file.type.startsWith("image/") || file.size > 10 * 1024 * 1024,
    );
    if (invalid) {
      notification.warn({
        title: "图片不可用",
        description: `${invalid.name} 不是受支持的图片或超过 10MB。`,
      });
      return;
    }
    setFiles(selected);
    setCapture(null);
    setDrafts([]);
  }, []);

  async function parse() {
    if (!files.length) {
      notification.warn({ title: "请先选择图片" });
      return;
    }
    setUploading(true);
    setCapture(null);
    setDrafts([]);
    try {
      const result = await createFinanceAiCapture(files, {
        review: true,
        idempotencyKey: `web-review-${crypto.randomUUID()}`,
      });
      setCapture(result);
      setDrafts(result.items);
      void loadHistory();
      if (result.status === "failed")
        notification.danger({
          title: "图片识别失败",
          description: result.errorMessage || "模型没有返回可用结果。",
        });
      else
        notification.success({
          title: "识别完成",
          description: `识别到 ${result.items.length} 条待审核账目。`,
        });
    } catch (cause) {
      notification.danger({
        title: "图片解析失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setUploading(false);
    }
  }

  function update(id: number, patch: Partial<Draft>) {
    setDrafts((items) =>
      items.map((item) => (item.id === id ? editable(item, patch) : item)),
    );
  }
  function reject(id: number) {
    update(id, { status: "rejected" });
  }
  function restore(id: number) {
    update(id, { status: "draft" });
  }

  async function confirm() {
    if (!capture) return;
    const active = drafts.filter((item) => item.status !== "rejected");
    if (
      active.some(
        (item) =>
          !item.amount ||
          !item.accountId ||
          !item.categoryId ||
          !item.occurredAt,
      )
    ) {
      notification.warn({
        title: "草稿尚未完整",
        description: "请补全金额、日期、账户和分类后再入账。",
      });
      return;
    }
    setConfirming(true);
    try {
      const result = await confirmFinanceAiCapture(
        capture.id,
        capturePayload(drafts),
      );
      setCapture(result);
      setDrafts(result.items);
      void loadHistory();
      notification.success({
        title: "已完成入账",
        description: "审核通过的草稿已写入账本。",
      });
    } catch (cause) {
      notification.danger({
        title: "入账失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setConfirming(false);
    }
  }

  function openHistory(item: FinanceAiCapture) {
    setHistorySelected(item);
    setHistoryDrafts(item.items);
  }

  function updateHistory(id: number, patch: Partial<Draft>) {
    setHistoryDrafts((items) => items.map((item) => (item.id === id ? editable(item, patch) : item)));
  }

  async function confirmHistory() {
    if (!historySelected) return;
    const active = historyDrafts.filter((item) => item.status !== "rejected");
    if (active.some((item) => !item.amount || !item.accountId || !item.categoryId || !item.occurredAt)) {
      notification.warn({ title: "账目尚未完整", description: "请补全金额、日期、账户和分类后再保存。" });
      return;
    }
    setConfirming(true);
    try {
      const result = await confirmFinanceAiCapture(historySelected.id, capturePayload(historyDrafts));
      setHistorySelected(result);
      setHistoryDrafts(result.items);
      setHistory((items) => items.map((item) => (item.id === result.id ? result : item)));
      notification.success({ title: result.status === "confirmed" ? "历史记录已更新" : "已保存识别草稿" });
    } catch (cause) {
      notification.danger({ title: "历史记录保存失败", description: cause instanceof Error ? cause.message : String(cause) });
    } finally {
      setConfirming(false);
    }
  }

  async function retryHistory() {
    if (!historySelected) return;
    setRetrying(true);
    try {
      const result = await retryFinanceAiCapture(historySelected.id);
      setHistorySelected(result);
      setHistoryDrafts(result.items);
      setHistory((items) => items.map((item) => (item.id === result.id ? result : item)));
      notification.success({ title: result.status === "failed" ? "重新识别仍未完成" : "已重新识别" });
    } catch (cause) {
      notification.danger({ title: "重新识别失败", description: cause instanceof Error ? cause.message : String(cause) });
    } finally {
      setRetrying(false);
    }
  }

  async function archiveHistory() {
    if (!historySelected) return;
    setArchiving(true);
    try {
      await archiveFinanceAiCapture(historySelected.id);
      const archived = { ...historySelected, status: "archived" as const, updatedAt: new Date().toISOString() };
      setHistorySelected(archived);
      setHistory((items) => items.map((item) => (item.id === archived.id ? archived : item)));
      notification.success({ title: "已归档 AI 记录" });
    } catch (cause) {
      notification.danger({ title: "归档失败", description: cause instanceof Error ? cause.message : String(cause) });
    } finally {
      setArchiving(false);
    }
  }

  const historyPageCount = Math.max(1, Math.ceil(historyTotal / historyPageSize));

  function changeHistoryPageSize(value: string) {
    setHistoryPageSize(Number(value));
    setHistoryPage(1);
  }

  return (
    <WorkspaceShell sectionLabel="Finance" pageTitle="AI Capture">
      <FinancePageHeader
        current="ai-capture"
        description="快捷指令调用 API 默认自动解析并入账；此页面用于查看和处理需要人工补审的图片。"
      />
      <div className="grid gap-5 lg:grid-cols-[minmax(280px,0.72fr)_minmax(0,1.28fr)]">
        <section className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card">
          <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3 sm:px-5">
            <div className="flex min-w-0 items-start gap-3">
              <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted text-foreground">
                <Camera className="size-4" />
              </span>
              <div className="min-w-0">
                <h2 className="text-sm font-semibold">图片入口</h2>
                <p className="mt-0.5 text-xs leading-5 text-muted-foreground">一张小票或账单，最大 10MB</p>
              </div>
            </div>
             <div className="flex min-w-0 items-center gap-1">
               <AiBindingProviderSelect capability="accounting_receipt_parse" />
               <Badge variant={files.length ? "secondary" : "outline"}>{files.length ? "已选择" : "待上传"}</Badge>
             </div>
          </div>
          <div className="space-y-4 p-4 sm:p-5">
            <label className="group flex min-h-44 cursor-pointer flex-col items-center justify-center rounded-md border border-dashed border-border bg-muted/25 px-5 text-center transition-colors hover:border-primary/50 hover:bg-muted/50 focus-within:ring-2 focus-within:ring-ring">
              <span className="flex size-10 items-center justify-center rounded-md bg-background text-muted-foreground shadow-sm">
                <FileImage className="size-5" />
              </span>
              <span className="mt-3 text-sm font-medium">选择小票或账单图片</span>
              <span className="mt-1 max-w-[240px] text-xs leading-5 text-muted-foreground">账户、分类和收支类型由 AI 自动识别</span>
              <input type="file" accept="image/*" className="sr-only" onChange={(event) => chooseFiles(event.target.files)} />
            </label>
            {previews.length ? (
              <div className="flex items-center gap-3 rounded-md bg-muted/35 p-2">
                {previews.map(({ file, url }) => (
                  <div key={`${file.name}-${file.lastModified}`} className="group relative size-16 shrink-0 overflow-hidden rounded-md border border-border/60 bg-muted">
                    <img src={url} alt={file.name} className="size-full object-cover" />
                    <button type="button" title="移除图片" aria-label={`移除 ${file.name}`} onClick={() => setFiles((items) => items.filter((item) => item !== file))} className="absolute right-1 top-1 rounded-md bg-black/60 p-1 text-white opacity-0 transition-opacity group-hover:opacity-100 focus-visible:opacity-100">
                      <X className="size-3.5" />
                    </button>
                  </div>
                ))}
                <div className="min-w-0">
                  <p className="truncate text-xs font-medium">{files[0]?.name}</p>
                  <p className="mt-1 text-[11px] text-muted-foreground">1 张图片已准备</p>
                </div>
              </div>
            ) : null}
          </div>
          <div className="flex items-center justify-between gap-3 border-t border-border/60 bg-muted/20 px-4 py-3 sm:px-5">
            <span className="text-xs text-muted-foreground">快捷指令和页面入口使用同一套解析流程</span>
            <Button type="button" onClick={() => void parse()} disabled={!files.length || uploading} className="shrink-0 gap-2">
              {uploading ? <LoaderCircle className="size-4 animate-spin" /> : <Sparkles className="size-4" />}
              {uploading ? "识别中" : "开始识别"}
            </Button>
          </div>
        </section>
        <section className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card">
          <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3 sm:px-5">
            <div className="flex min-w-0 items-start gap-3">
              <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted text-foreground">
                <ReceiptText className="size-4" />
              </span>
              <div className="min-w-0">
                <h2 className="text-sm font-semibold">审核草稿</h2>
                <p className="mt-0.5 text-xs leading-5 text-muted-foreground">确认金额、日期、账户和分类后才会入账</p>
              </div>
            </div>
            {capture ? <Badge variant={capture.status === "failed" ? "destructive" : capture.status === "review" ? "secondary" : "outline"}>{statusLabels[capture.status]}</Badge> : <Badge variant="outline">等待识别</Badge>}
          </div>
          {uploading ? (
            <div className="space-y-3 p-4 sm:p-5"><Skeleton className="h-24" /><Skeleton className="h-24" /><Skeleton className="h-24" /></div>
          ) : capture?.status === "failed" ? (
            <div className="p-4 sm:p-5"><Alert variant="destructive"><AlertCircle className="size-4" /><AlertTitle>图片识别失败</AlertTitle><AlertDescription>{capture.errorMessage || "模型未返回可用结果，可在识别历史中重试。"}</AlertDescription></Alert></div>
          ) : drafts.length ? (
            <div className="space-y-3 p-4 sm:p-5">
              {drafts.map((item) => <DraftRow key={item.id} item={item} accounts={accounts} categories={categories} onUpdate={update} onReject={reject} onRestore={restore} />)}
              <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border/60 pt-4">
                <span className="text-xs text-muted-foreground">已识别 {drafts.length} 条 · 拒绝的草稿不会入账</span>
                <Button type="button" onClick={() => void confirm()} disabled={confirming || capture?.status !== "review"} className="gap-2"><Check className="size-4" />{confirming ? "入账中" : "确认并入账"}</Button>
              </div>
            </div>
          ) : (
            <div className="flex min-h-64 flex-col items-center justify-center bg-muted/10 px-6 text-center">
              <span className="flex size-10 items-center justify-center rounded-md bg-muted text-muted-foreground"><ReceiptText className="size-5" /></span>
              <p className="mt-3 text-sm font-medium">等待识别结果</p>
              <p className="mt-1 max-w-sm text-xs leading-5 text-muted-foreground">识别结果会先停留在这里，审核无误后再提交。</p>
            </div>
          )}
        </section>
      </div>
      <section className="overflow-hidden rounded-lg border border-border/70 bg-card">
        <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3 sm:px-5">
          <div className="flex min-w-0 items-start gap-3">
            <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted text-muted-foreground"><FileClock className="size-4" /></span>
            <div className="min-w-0"><h2 className="text-sm font-semibold">识别历史</h2><p className="mt-0.5 text-xs leading-5 text-muted-foreground">每次图片上传都会留下记录，失败记录可以保留原图重新识别。</p></div>
          </div>
          <Button type="button" variant="ghost" size="icon-sm" title="刷新历史" aria-label="刷新历史" onClick={() => void loadHistory()} disabled={historyLoading}><RefreshCw className={historyLoading ? "size-4 animate-spin" : "size-4"} /></Button>
        </div>
        {historyLoading ? (
          <div className="divide-y divide-border/60">
            <div className="h-12 animate-pulse bg-muted/35" />
            <div className="h-12 animate-pulse bg-muted/20" />
            <div className="h-12 animate-pulse bg-muted/35" />
          </div>
        ) : history.length ? (
          <>
            <div className="hidden overflow-x-auto md:block">
              <Table className="min-w-[760px] table-fixed">
                <colgroup>
                  <col className="w-[250px]" />
                  <col className="w-[116px]" />
                  <col />
                  <col className="w-[168px]" />
                  <col className="w-16" />
                </colgroup>
                <TableHeader className="[&_th]:bg-muted">
                  <TableRow>
                    <TableHead>识别内容</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>结果</TableHead>
                    <TableHead>创建时间</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {history.map((item) => {
                    const confirmed = item.items.filter((draft) => draft.status === "confirmed").length;
                    const income = item.items[0]?.transactionType === "income";
                    return <TableRow key={item.id} className="cursor-pointer" onClick={() => openHistory(item)}>
                      <TableCell>
                        <div className="flex min-w-0 items-center gap-2">
                          <span className={`flex size-7 shrink-0 items-center justify-center rounded-md ${income ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300" : "bg-rose-500/10 text-rose-700 dark:text-rose-300"}`}>{income ? <ArrowDownLeft className="size-3.5" /> : <ArrowUpRight className="size-3.5" />}</span>
                          <span className="min-w-0 truncate font-medium" title={item.items[0]?.merchant || item.items[0]?.categoryHint || "图片记账"}>{item.items[0]?.merchant || item.items[0]?.categoryHint || "图片记账"}</span>
                        </div>
                      </TableCell>
                      <TableCell><Badge variant={item.status === "failed" ? "destructive" : item.status === "review" ? "secondary" : "outline"}>{statusLabels[item.status]}</Badge></TableCell>
                      <TableCell className="text-xs text-muted-foreground">{item.status === "failed" ? <span className="block max-w-[180px] truncate" title={item.errorMessage || undefined}>{item.errorMessage || "模型未返回可用结果"}</span> : confirmed ? `${confirmed} 条已入账` : `${item.items.length} 条待处理`}</TableCell>
                      <TableCell className="font-mono text-xs text-muted-foreground">{formatHistoryDate(item.createdAt)}</TableCell>
                      <TableCell className="text-right"><ChevronRight className="ml-auto size-4 text-muted-foreground" /></TableCell>
                    </TableRow>;
                  })}
                </TableBody>
              </Table>
            </div>
            <div className="divide-y divide-border/60 md:hidden">
              {history.map((item) => {
                const confirmed = item.items.filter((draft) => draft.status === "confirmed").length;
                const income = item.items[0]?.transactionType === "income";
                return <button key={item.id} type="button" onClick={() => openHistory(item)} className="group flex w-full min-w-0 items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/40 focus-visible:bg-muted/50 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-inset focus-visible:ring-ring">
                  <span className={`flex size-8 shrink-0 items-center justify-center rounded-md ${income ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300" : "bg-rose-500/10 text-rose-700 dark:text-rose-300"}`}>{income ? <ArrowDownLeft className="size-4" /> : <ArrowUpRight className="size-4" />}</span>
                  <div className="min-w-0 flex-1"><div className="flex min-w-0 items-center gap-2"><span className="truncate text-sm font-medium">{item.items[0]?.merchant || item.items[0]?.categoryHint || "图片记账"}</span><Badge variant={item.status === "failed" ? "destructive" : item.status === "review" ? "secondary" : "outline"} className="shrink-0">{statusLabels[item.status]}</Badge></div><div className="mt-1 truncate text-xs text-muted-foreground">{item.status === "failed" ? item.errorMessage || "模型未返回可用结果" : confirmed ? `${confirmed} 条已入账` : `${item.items.length} 条结果 · ${formatHistoryDate(item.createdAt)}`}</div></div>
                  <ChevronRight className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
                </button>;
              })}
            </div>
          </>
        ) : (
          <div className="flex min-h-32 flex-col items-center justify-center px-6 text-center"><FileClock className="size-7 text-muted-foreground/50" /><p className="mt-2 text-sm font-medium">还没有图片识别记录</p><p className="mt-1 text-xs text-muted-foreground">通过快捷指令或上方上传入口创建第一条记录。</p></div>
        )}
        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border/60 px-4 py-3 sm:px-5">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span>共 {historyTotal} 次识别</span>
            <span className="hidden text-border sm:inline">·</span>
            <span className="hidden sm:inline">第 {historyPage} / {historyPageCount} 页</span>
          </div>
          <div className="flex w-full items-center justify-between gap-3 sm:w-auto sm:justify-end">
            <div className="flex items-center gap-2">
              <span className="text-xs text-muted-foreground">Rows</span>
              <Select value={`${historyPageSize}`} onValueChange={changeHistoryPageSize}>
                <SelectTrigger size="sm" className="w-20" aria-label="每页条数"><SelectValue /></SelectTrigger>
                <SelectContent side="top"><SelectItem value="10">10</SelectItem><SelectItem value="20">20</SelectItem><SelectItem value="30">30</SelectItem><SelectItem value="50">50</SelectItem></SelectContent>
              </Select>
            </div>
            <div className="flex items-center gap-1.5">
              <Button type="button" variant="outline" size="icon" className="hidden size-8 sm:inline-flex" onClick={() => setHistoryPage(1)} disabled={historyPage <= 1} aria-label="第一页"><ChevronsLeft className="size-4" /></Button>
              <Button type="button" variant="outline" size="icon" className="size-8" onClick={() => setHistoryPage((current) => Math.max(1, current - 1))} disabled={historyPage <= 1} aria-label="上一页"><ChevronLeft className="size-4" /></Button>
              <span className="min-w-16 text-center text-xs font-medium tabular-nums">{historyPage} / {historyPageCount}</span>
              <Button type="button" variant="outline" size="icon" className="size-8" onClick={() => setHistoryPage((current) => Math.min(historyPageCount, current + 1))} disabled={historyPage >= historyPageCount} aria-label="下一页"><ChevronRight className="size-4" /></Button>
              <Button type="button" variant="outline" size="icon" className="hidden size-8 sm:inline-flex" onClick={() => setHistoryPage(historyPageCount)} disabled={historyPage >= historyPageCount} aria-label="最后一页"><ChevronsRight className="size-4" /></Button>
            </div>
          </div>
        </div>
      </section>
      <Drawer open={historySelected != null} onOpenChange={(open) => !open && setHistorySelected(null)} direction={isMobile ? "bottom" : "right"}>
        {historySelected ? (
          <DrawerContent className="overflow-hidden p-0 data-[vaul-drawer-direction=bottom]:max-h-[min(92dvh,860px)] data-[vaul-drawer-direction=right]:bottom-0 data-[vaul-drawer-direction=right]:left-auto data-[vaul-drawer-direction=right]:right-0 data-[vaul-drawer-direction=right]:top-0 data-[vaul-drawer-direction=right]:h-full data-[vaul-drawer-direction=right]:max-h-none data-[vaul-drawer-direction=right]:w-full data-[vaul-drawer-direction=right]:sm:w-[min(680px,calc(100vw-24px))] data-[vaul-drawer-direction=right]:sm:max-w-none">
            <DrawerHeader className="border-b border-border/60 px-4 pb-4 pt-3 text-left sm:px-6">
              <div className="flex items-start justify-between gap-3 pr-8">
                <div className="min-w-0">
                  <DrawerTitle className="truncate text-base">图片识别详情</DrawerTitle>
                  <DrawerDescription className="mt-1">{formatHistoryDate(historySelected.createdAt)} · {historySelected.sourceCount} 张图片 · {historySelected.items.length} 条结果</DrawerDescription>
                </div>
                <span className={`shrink-0 rounded-md px-2 py-1 text-xs font-medium ${statusClass(historySelected.status)}`}>{statusLabels[historySelected.status]}</span>
              </div>
              {historySelected.errorMessage ? <p className="mt-3 rounded-md bg-rose-500/10 px-3 py-2 text-xs leading-5 text-rose-700 dark:text-rose-300">{historySelected.errorMessage}</p> : null}
            </DrawerHeader>
            <ScrollArea className="min-h-0 flex-1">
              {historyDrafts.length ? (
                <div className="space-y-3 p-4 sm:p-6">
                  {historyDrafts.map((item) => <DraftRow key={item.id} item={item} accounts={accounts} categories={categories} onUpdate={updateHistory} onReject={(id) => updateHistory(id, { status: "rejected" })} onRestore={(id) => updateHistory(id, { status: "draft" })} />)}
                </div>
              ) : <div className="flex min-h-48 items-center justify-center px-6 text-center text-sm text-muted-foreground">这次识别没有生成可编辑账目。</div>}
            </ScrollArea>
            <DrawerFooter className="border-t border-border/60 bg-background/95 sm:flex-row sm:justify-between">
              <div className="flex gap-1">
                {historySelected.status === "failed" ? <Button type="button" variant="outline" className="gap-2" onClick={() => void retryHistory()} disabled={retrying}><RefreshCw className={retrying ? "size-4 animate-spin" : "size-4"} />{retrying ? "重新识别中" : "重新识别"}</Button> : null}
                {historySelected.status !== "archived" ? <Button type="button" variant="ghost" className="gap-2" onClick={() => void archiveHistory()} disabled={archiving}><Archive className="size-4" />归档</Button> : null}
              </div>
              <div className="flex gap-2">
                <DrawerClose asChild><Button type="button" variant="ghost">关闭</Button></DrawerClose>
                {historyDrafts.length && historySelected.status !== "archived" && historySelected.status !== "processing" ? <Button type="button" className="gap-2" onClick={() => void confirmHistory()} disabled={confirming}><Check className="size-4" />{confirming ? "保存中" : historySelected.status === "confirmed" ? "保存修改" : "确认并入账"}</Button> : null}
              </div>
            </DrawerFooter>
          </DrawerContent>
        ) : null}
      </Drawer>
    </WorkspaceShell>
  );
}

function formatHistoryDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function DraftRow({
  item,
  accounts,
  categories,
  onUpdate,
  onReject,
  onRestore,
}: {
  item: Draft;
  accounts: FinanceAccount[];
  categories: FinanceCategory[];
  onUpdate: (id: number, patch: Partial<Draft>) => void;
  onReject: (id: number) => void;
  onRestore: (id: number) => void;
}) {
  const rejected = item.status === "rejected";
  const posted = item.transactionId != null || item.status === "confirmed";
  const availableCategories = categories.filter(
    (category) =>
      category.direction === "both" ||
      category.direction === item.transactionType,
  );
  const selectedAccount = accounts.find(
    (account) => account.id === item.accountId,
  );
  const credit = selectedAccount?.nature === "credit";
  return (
    <div
      className={`rounded-lg border p-3 ${rejected ? "border-border/50 bg-muted/30 opacity-60" : "border-border/70 bg-background"}`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2">
          <span
            className={`rounded px-1.5 py-0.5 text-[11px] font-medium ${item.transactionType === "income" ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300" : "bg-rose-500/10 text-rose-700 dark:text-rose-300"}`}
          >
            {item.transactionType === "income" ? "收入" : "支出"}
          </span>
          <span className="truncate text-sm font-medium">
            {item.merchant || item.categoryHint || "未命名账目"}
          </span>
          {item.confidence != null ? (
            <span className="text-[11px] text-muted-foreground">
              置信度 {(item.confidence * 100).toFixed(0)}%
            </span>
          ) : null}
        </div>
        {rejected ? (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onRestore(item.id)}
          >
            恢复
          </Button>
        ) : posted ? (
          <span className="rounded-md bg-emerald-500/10 px-2 py-1 text-[11px] font-medium text-emerald-700 dark:text-emerald-300">已入账</span>
        ) : (
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            title="拒绝此草稿"
            aria-label="拒绝此草稿"
            onClick={() => onReject(item.id)}
          >
            <Trash2 className="size-4" />
          </Button>
        )}
      </div>
      {!rejected ? (
        <div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
          <label className="space-y-1 text-[11px] font-medium">
            金额
            <Input
              type="number"
              min="0.01"
              step="0.01"
              value={item.amount ?? ""}
              onChange={(event) =>
                onUpdate(item.id, {
                  amount: event.target.value
                    ? Number(event.target.value)
                    : null,
                })
              }
            />
          </label>
          <label className="space-y-1 text-[11px] font-medium">
            日期
            <Input
              type="datetime-local"
              value={item.occurredAt ? item.occurredAt.slice(0, 16) : ""}
              onChange={(event) =>
                onUpdate(item.id, {
                  occurredAt: event.target.value
                    ? `${event.target.value}:00+08:00`
                    : null,
                })
              }
            />
          </label>
          <label className="space-y-1 text-[11px] font-medium">
            账户
            <select
              required
              value={item.accountId ?? ""}
              onChange={(event) =>
                onUpdate(item.id, {
                  accountId: event.target.value
                    ? Number(event.target.value)
                    : null,
                })
              }
              className="h-9 w-full rounded-md border border-input bg-background px-2 text-sm"
            >
              <option value="">选择账户</option>
              {accounts
                .filter((account) => !account.archived)
                .map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.name}
                  </option>
                ))}
            </select>
          </label>
          <label className="space-y-1 text-[11px] font-medium">
            分类
            <select
              required
              value={item.categoryId ?? ""}
              onChange={(event) =>
                onUpdate(item.id, {
                  categoryId: event.target.value
                    ? Number(event.target.value)
                    : null,
                })
              }
              className="h-9 w-full rounded-md border border-input bg-background px-2 text-sm"
            >
              <option value="">选择分类</option>
              {availableCategories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
          {credit && item.transactionType === "expense" ? (
            <>
              <label className="space-y-1 text-[11px] font-medium">
                分期期数
                <Input
                  type="number"
                  min="2"
                  max="60"
                  step="1"
                  value={item.installmentCount ?? ""}
                  onChange={(event) =>
                    onUpdate(item.id, {
                      installmentCount: event.target.value
                        ? Number(event.target.value)
                        : null,
                    })
                  }
                  placeholder="可选"
                />
              </label>
              <label className="space-y-1 text-[11px] font-medium">
                分期费用
                <Input
                  type="number"
                  min="0"
                  step="0.01"
                  value={item.installmentFee ?? ""}
                  onChange={(event) =>
                    onUpdate(item.id, {
                      installmentFee: event.target.value
                        ? Number(event.target.value)
                        : null,
                    })
                  }
                  placeholder="0.00"
                />
              </label>
            </>
          ) : null}
          <label className="space-y-1 text-[11px] font-medium">
            商户 / 来源
            <Input
              value={item.merchant ?? ""}
              onChange={(event) =>
                onUpdate(item.id, { merchant: event.target.value })
              }
              placeholder="可选"
            />
          </label>
          <label className="space-y-1 text-[11px] font-medium">
            备注
            <Input
              value={item.note ?? ""}
              onChange={(event) =>
                onUpdate(item.id, { note: event.target.value })
              }
              placeholder="可选"
            />
          </label>
        </div>
      ) : (
        <div className="mt-2 text-xs text-muted-foreground">
          {formatMoney(item.amount)} · {item.merchant || "未命名"} ·
          已标记为拒绝
        </div>
      )}
    </div>
  );
}
