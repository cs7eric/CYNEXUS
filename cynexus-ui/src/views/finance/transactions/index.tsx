import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
} from "react";
import {
  Archive,
  ArrowDownLeft,
  ArrowLeftRight,
  ArrowUpRight,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  Search,
  Trash2,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { notification } from "@/components/ui/notification";
import { WorkspaceShell } from "@/layout/workspace-shell";
import { useIsMobile } from "@/hooks/use-mobile";
import {
  archiveFinanceTransaction,
  deleteFinanceTransaction,
  fetchFinanceAccounts,
  fetchFinanceCategories,
  fetchFinanceTransactions,
  updateFinanceTransaction,
  type FinanceAccount,
  type FinanceCategory,
  type FinanceTransaction,
} from "@/services/finance/ledger";
import { FinancePageHeader } from "../components/page-header";
import { formatDateTime, formatMoney, monthBounds } from "../lib/format";

export default function FinanceTransactionsPage() {
  const isMobile = useIsMobile();
  const [range, setRange] = useState(monthBounds());
  const [rows, setRows] = useState<FinanceTransaction[]>([]);
  const [accounts, setAccounts] = useState<FinanceAccount[]>([]);
  const [categories, setCategories] = useState<FinanceCategory[]>([]);
  const [type, setType] = useState<
    "all" | FinanceTransaction["transactionType"]
  >("all");
  const [accountId, setAccountId] = useState("all");
  const [categoryId, setCategoryId] = useState("all");
  const [search, setSearch] = useState("");
  const [pageSize, setPageSize] = useState(25);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<FinanceTransaction | null>(null);
  const [editing, setEditing] = useState<FinanceTransaction | null>(null);
  const [deleting, setDeleting] = useState<FinanceTransaction | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [nextRows, nextAccounts, nextCategories] = await Promise.all([
        fetchFinanceTransactions({
          start: range.start,
          end: range.end,
          limit: 200,
        }),
        fetchFinanceAccounts(false),
        fetchFinanceCategories(true),
      ]);
      setRows(nextRows);
      setAccounts(nextAccounts);
      setCategories(nextCategories);
      setPage(1);
      setSelected((current) =>
        current
          ? (nextRows.find((row) => row.id === current.id) ?? null)
          : null,
      );
    } catch (cause) {
      notification.danger({
        title: "账目加载失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setLoading(false);
    }
  }, [range]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const filteredRows = useMemo(() => {
    const term = search.trim().toLowerCase();
    return rows.filter((row) => {
      if (type !== "all" && row.transactionType !== type) return false;
      if (
        accountId !== "all" &&
        row.accountId !== Number(accountId) &&
        row.toAccountId !== Number(accountId)
      )
        return false;
      if (categoryId !== "all" && row.categoryId !== Number(categoryId))
        return false;
      if (
        term &&
        ![
          row.merchant,
          row.note,
          row.categoryName,
          row.accountName,
          row.toAccountName,
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase()
          .includes(term)
      )
        return false;
      return true;
    });
  }, [accountId, categoryId, rows, search, type]);

  const pageCount = Math.max(1, Math.ceil(filteredRows.length / pageSize));
  const visibleRows = filteredRows.slice(
    (page - 1) * pageSize,
    page * pageSize,
  );

  function openEdit(row: FinanceTransaction) {
    setSelected(row);
    setEditing(row);
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editing) return;
    const form = new FormData(event.currentTarget);
    const transactionType = String(
      form.get("transactionType"),
    ) as FinanceTransaction["transactionType"];
    const amount = Number(form.get("amount"));
    const nextAccountId = Number(form.get("accountId"));
    const nextToAccountId = form.get("toAccountId")
      ? Number(form.get("toAccountId"))
      : null;
    const nextCategoryId = form.get("categoryId")
      ? Number(form.get("categoryId"))
      : null;
    const occurredAt = String(form.get("occurredAt"));
    if (
      !Number.isFinite(amount) ||
      amount <= 0 ||
      !nextAccountId ||
      !occurredAt ||
      (transactionType !== "transfer" && !nextCategoryId) ||
      (transactionType === "transfer" && !nextToAccountId)
    ) {
      notification.danger({
        title: "请补全账目信息",
        description: "金额、时间、账户和收支分类必须填写；转账还需要目标账户。",
      });
      return;
    }
    setSaving(true);
    try {
      await updateFinanceTransaction(editing.id, {
        transactionType,
        accountId: nextAccountId,
        toAccountId: transactionType === "transfer" ? nextToAccountId : null,
        categoryId: transactionType === "transfer" ? null : nextCategoryId,
        amount,
        occurredAt: `${occurredAt}:00+08:00`,
        merchant: String(form.get("merchant") || "").trim() || null,
        note: String(form.get("note") || "").trim() || null,
      });
      setEditing(null);
      notification.success({ title: "账目已更新" });
      await load();
    } catch (cause) {
      notification.danger({
        title: "账目更新失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (!deleting) return;
    try {
      await deleteFinanceTransaction(deleting.id);
      setDeleting(null);
      setSelected(null);
      notification.success({ title: "账目已删除" });
      await load();
    } catch (cause) {
      notification.danger({
        title: "账目删除失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    }
  }

  async function archive(id: number) {
    try {
      await archiveFinanceTransaction(id);
      if (selected?.id === id) setSelected(null);
      notification.success({ title: "账目已归档" });
      await load();
    } catch (cause) {
      notification.danger({
        title: "账目归档失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    }
  }

  return (
    <WorkspaceShell
      sectionLabel="Finance"
      pageTitle="Transactions"
      contentScrollable={false}
    >
      <FinancePageHeader
        current="transactions"
        description="集中查看、修正和删除已经入账的收入、支出与转账。"
        onRefresh={() => void load()}
        loading={loading}
      />
      <section className="rounded-xl border border-border/70 bg-card p-4">
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-[minmax(140px,0.8fr)_minmax(140px,0.8fr)_minmax(130px,0.7fr)_minmax(170px,0.95fr)_minmax(150px,0.85fr)_minmax(240px,1.4fr)]">
          <FilterField label="开始日期">
            <Input
              className="w-full"
              type="date"
              name="start"
              value={range.start}
              onChange={(event) =>
                setRange((value) => ({ ...value, start: event.target.value }))
              }
            />
          </FilterField>
          <FilterField label="结束日期">
            <Input
              className="w-full"
              type="date"
              name="end"
              value={range.end}
              onChange={(event) =>
                setRange((value) => ({ ...value, end: event.target.value }))
              }
            />
          </FilterField>
          <FilterField label="类型">
            <Select
              value={type}
              onValueChange={(value) => {
                setType(value as typeof type);
                setPage(1);
              }}
            >
              <SelectTrigger
                size="sm"
                className="w-full min-w-0 border-0 bg-muted/45 px-3 text-sm shadow-none hover:bg-muted/70"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent
                className="max-h-72"
                style={{ maxHeight: "240px" }}
              >
                <SelectItem value="all">全部类型</SelectItem>
                <SelectItem value="expense">支出</SelectItem>
                <SelectItem value="income">收入</SelectItem>
                <SelectItem value="transfer">转账</SelectItem>
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="账户">
            <Select
              value={accountId}
              onValueChange={(value) => {
                setAccountId(value);
                setPage(1);
              }}
            >
              <SelectTrigger
                size="sm"
                className="w-full min-w-0 border-0 bg-muted/45 px-3 text-sm shadow-none hover:bg-muted/70"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent
                className="max-h-72"
                style={{ maxHeight: "240px" }}
              >
                <SelectItem value="all">全部账户</SelectItem>
                {accounts.map((item) => (
                  <SelectItem key={item.id} value={String(item.id)}>
                    {item.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="分类">
            <Select
              value={categoryId}
              onValueChange={(value) => {
                setCategoryId(value);
                setPage(1);
              }}
            >
              <SelectTrigger
                size="sm"
                className="w-full min-w-0 border-0 bg-muted/45 px-3 text-sm shadow-none hover:bg-muted/70"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent
                className="max-h-72"
                style={{ maxHeight: "240px" }}
              >
                <SelectItem value="all">全部分类</SelectItem>
                {categories
                  .filter((item) => !item.archived)
                  .map((item) => (
                    <SelectItem key={item.id} value={String(item.id)}>
                      {item.name}
                    </SelectItem>
                  ))}
              </SelectContent>
            </Select>
          </FilterField>
          <FilterField label="搜索" className="sm:col-span-2 xl:col-span-1">
            <div className="relative">
              <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(1);
                }}
                className="w-full pl-8"
                placeholder="商户、备注、账户或分类"
              />
            </div>
          </FilterField>
        </div>
      </section>
      <section className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-md bg-muted/50">
        <div className="flex items-center justify-between gap-3 border-b border-border/60 px-4 py-3">
          <div>
            <h2 className="text-sm font-semibold">账目列表</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              共 {filteredRows.length} 条，每页 {pageSize}{" "}
              条；点击账目查看详情。
            </p>
          </div>
          <div className="shrink-0 text-xs text-muted-foreground">
            第 {page} / {pageCount} 页
          </div>
        </div>
        <div className="min-h-0 flex-1 overflow-auto">
          {loading ? (
            <div className="space-y-3 p-3 sm:p-4">
              <Skeleton className="h-20" />
              <Skeleton className="h-20" />
              <Skeleton className="h-20" />
              <Skeleton className="h-20" />
            </div>
          ) : visibleRows.length ? (
            isMobile ? (
              <div className="space-y-2 p-3">
                {visibleRows.map((row) => (
                  <TransactionCard
                    key={row.id}
                    row={row}
                    onSelect={setSelected}
                    onEdit={openEdit}
                  />
                ))}
              </div>
            ) : (
              <Table className="min-w-[680px]">
                <TableHeader className="[&_th]:sticky [&_th]:top-0 [&_th]:z-20 [&_th]:bg-muted">
                  <TableRow>
                    <TableHead>时间</TableHead>
                    <TableHead>账目</TableHead>
                    <TableHead>账户</TableHead>
                    <TableHead>分类</TableHead>
                    <TableHead className="text-right">金额</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {visibleRows.map((row) => (
                    <TransactionRow
                      key={row.id}
                      row={row}
                      onSelect={setSelected}
                      onEdit={openEdit}
                    />
                  ))}
                </TableBody>
              </Table>
            )
          ) : (
            <div className="flex h-full items-center justify-center px-4 text-center text-sm text-muted-foreground">
              <div>
                <CalendarDays className="mx-auto size-8 text-muted-foreground/45" />
                <p className="mt-3 text-sm font-medium">没有匹配的账目</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  调整日期或筛选条件后再试。
                </p>
              </div>
            </div>
          )}
        </div>
        <div className="flex shrink-0 items-center justify-between gap-4 border-t border-border/60 px-4 py-3">
          <div className="flex items-center gap-2">
            <span className="hidden text-sm text-muted-foreground sm:inline">
              每页
            </span>
            <Select
              value={String(pageSize)}
              onValueChange={(value) => {
                setPageSize(Number(value));
                setPage(1);
              }}
            >
              <SelectTrigger
                size="sm"
                className="w-20 border-0 bg-muted/45 px-2 text-xs shadow-none hover:bg-muted/70"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent
                side="top"
                className="max-h-72"
                style={{ maxHeight: "240px" }}
              >
                <SelectItem value="10">10</SelectItem>
                <SelectItem value="20">20</SelectItem>
                <SelectItem value="25">25</SelectItem>
                <SelectItem value="30">30</SelectItem>
                <SelectItem value="50">50</SelectItem>
              </SelectContent>
            </Select>
            <span className="hidden text-sm text-muted-foreground sm:inline">
              条
            </span>
          </div>
          <div className="flex items-center gap-5">
            <div className="text-sm font-medium">
              第 {page} / {pageCount} 页
            </div>
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="icon-sm"
                className="hidden lg:inline-flex"
                title="第一页"
                aria-label="第一页"
                disabled={page === 1}
                onClick={() => setPage(1)}
              >
                <ChevronsLeft className="size-4" />
              </Button>
              <Button
                type="button"
                variant="outline"
                size="icon-sm"
                title="上一页"
                aria-label="上一页"
                disabled={page === 1}
                onClick={() => setPage((value) => Math.max(1, value - 1))}
              >
                <ChevronLeft className="size-4" />
              </Button>
              <Button
                type="button"
                variant="outline"
                size="icon-sm"
                title="下一页"
                aria-label="下一页"
                disabled={page === pageCount}
                onClick={() =>
                  setPage((value) => Math.min(pageCount, value + 1))
                }
              >
                <ChevronRight className="size-4" />
              </Button>
              <Button
                type="button"
                variant="outline"
                size="icon-sm"
                className="hidden lg:inline-flex"
                title="最后一页"
                aria-label="最后一页"
                disabled={page === pageCount}
                onClick={() => setPage(pageCount)}
              >
                <ChevronsRight className="size-4" />
              </Button>
            </div>
          </div>
        </div>
      </section>
      <TransactionDetailDrawer
        row={selected}
        editing={editing != null}
        accounts={accounts}
        categories={categories}
        saving={saving}
        open={selected != null}
        onOpenChange={(open) => {
          if (!open) {
            setSelected(null);
            setEditing(null);
          }
        }}
        onEdit={openEdit}
        onCancelEdit={() => {
          if (!saving) setEditing(null);
        }}
        onArchive={archive}
        onDelete={setDeleting}
        onSubmit={save}
      />
      <ConfirmDialog
        open={deleting != null}
        onOpenChange={(open) => {
          if (!open) setDeleting(null);
        }}
        title="删除这条账目？"
        description="删除后它不会再计入账户余额和消费统计。若这是分期消费，将同步删除分期计划；若这是分期还款，将撤销还款并把对应期次恢复为待还。"
        confirmText="删除账目"
        destructive
        onConfirm={remove}
      />
    </WorkspaceShell>
  );
}

function TransactionRow({
  row,
  onSelect,
  onEdit,
}: {
  row: FinanceTransaction;
  onSelect: (row: FinanceTransaction) => void;
  onEdit: (row: FinanceTransaction) => void;
}) {
  const income = row.transactionType === "income";
  const transfer = row.transactionType === "transfer";
  const Icon = transfer
    ? ArrowLeftRight
    : income
      ? ArrowDownLeft
      : ArrowUpRight;
  const tone = transfer
    ? "text-sky-600"
    : income
      ? "text-emerald-600"
      : "text-rose-600";
  const clickTimer = useRef<number | null>(null);
  useEffect(
    () => () => {
      if (clickTimer.current != null) window.clearTimeout(clickTimer.current);
    },
    [],
  );
  function select() {
    onSelect(row);
  }
  function selectAfterClick() {
    if (clickTimer.current != null) window.clearTimeout(clickTimer.current);
    clickTimer.current = window.setTimeout(() => {
      clickTimer.current = null;
      select();
    }, 220);
  }
  function editOnDoubleClick() {
    if (clickTimer.current != null) {
      window.clearTimeout(clickTimer.current);
      clickTimer.current = null;
    }
    onSelect(row);
    onEdit(row);
  }
  return (
    <TableRow
      className="cursor-pointer focus-visible:bg-muted focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-inset focus-visible:ring-ring/50"
      tabIndex={0}
      role="button"
      aria-label={`查看账目 ${row.merchant || row.categoryName || row.id}`}
      onClick={selectAfterClick}
      onDoubleClick={editOnDoubleClick}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          select();
        }
      }}
    >
      <TableCell className="text-xs text-muted-foreground">
        {formatDateTime(row.occurredAt)}
      </TableCell>
      <TableCell>
        <div className="flex items-center gap-2">
          <Icon className={`size-3.5 ${tone}`} />
          <div className="min-w-0">
            <div className="truncate font-medium">
              {row.merchant ||
                (transfer ? "账户转账" : row.categoryName || "未命名账目")}
            </div>
            <div className="truncate text-[11px] text-muted-foreground">
              {transfer
                ? `${row.accountName || "—"} → ${row.toAccountName || "—"}`
                : `${row.categoryName || "未分类"}${row.note ? ` · ${row.note}` : ""}`}
            </div>
          </div>
        </div>
      </TableCell>
      <TableCell className="text-xs text-muted-foreground">
        {transfer
          ? `${row.accountName || "—"} → ${row.toAccountName || "—"}`
          : row.accountName || "—"}
      </TableCell>
      <TableCell className="text-xs text-muted-foreground">
        {transfer ? "转账" : row.categoryName || "未分类"}
      </TableCell>
      <TableCell
        className={`text-right font-mono text-sm tabular-nums ${tone}`}
      >
        {transfer ? "" : income ? "+" : "-"}
        {formatMoney(row.amount)}
      </TableCell>
    </TableRow>
  );
}

function TransactionCard({
  row,
  onSelect,
  onEdit,
}: {
  row: FinanceTransaction;
  onSelect: (row: FinanceTransaction) => void;
  onEdit: (row: FinanceTransaction) => void;
}) {
  const income = row.transactionType === "income";
  const transfer = row.transactionType === "transfer";
  const Icon = transfer
    ? ArrowLeftRight
    : income
      ? ArrowDownLeft
      : ArrowUpRight;
  const tone = transfer
    ? "text-sky-600"
    : income
      ? "text-emerald-600"
      : "text-rose-600";
  const timer = useRef<number | null>(null);
  useEffect(
    () => () => {
      if (timer.current != null) window.clearTimeout(timer.current);
    },
    [],
  );
  function click() {
    if (timer.current != null) window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => {
      timer.current = null;
      onSelect(row);
    }, 220);
  }
  function doubleClick() {
    if (timer.current != null) window.clearTimeout(timer.current);
    timer.current = null;
    onEdit(row);
  }
  return (
    <div
      role="button"
      tabIndex={0}
      aria-label="查看账目"
      className="rounded-lg border border-border/70 bg-card px-3 py-3 text-left shadow-sm shadow-black/[0.02] transition-colors active:bg-muted/50 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
      onClick={click}
      onDoubleClick={doubleClick}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          onSelect(row);
        }
      }}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2">
          <span
            className={
              "flex size-8 shrink-0 items-center justify-center rounded-md bg-muted " +
              tone
            }
          >
            <Icon className="size-4" />
          </span>
          <div className="min-w-0">
            <div className="truncate text-sm font-medium">
              {row.merchant ||
                (transfer ? "账户转账" : row.categoryName || "未命名账目")}
            </div>
            <div className="mt-0.5 truncate text-xs text-muted-foreground">
              {formatDateTime(row.occurredAt)}
            </div>
          </div>
        </div>
        <span
          className={
            "shrink-0 font-mono text-sm font-semibold tabular-nums " + tone
          }
        >
          {transfer ? "—" : income ? "+" : "-"}
          {formatMoney(row.amount)}
        </span>
      </div>
      <div className="mt-3 flex min-w-0 items-center justify-between gap-3 border-t border-border/50 pt-2 text-xs text-muted-foreground">
        <span className="min-w-0 truncate">
          {transfer ? row.accountName || "—" : row.accountName || "—"}
        </span>
        <span className="shrink-0 truncate">
          {transfer ? "转账" : row.categoryName || "未分类"}
        </span>
      </div>
    </div>
  );
}

function TransactionDetailDrawer({
  row,
  editing,
  accounts,
  categories,
  saving,
  open,
  onOpenChange,
  onEdit,
  onCancelEdit,
  onArchive,
  onDelete,
  onSubmit,
}: {
  row: FinanceTransaction | null;
  editing: boolean;
  accounts: FinanceAccount[];
  categories: FinanceCategory[];
  saving: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit: (row: FinanceTransaction) => void;
  onCancelEdit: () => void;
  onArchive: (id: number) => Promise<void>;
  onDelete: (row: FinanceTransaction) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  const isMobile = useIsMobile();
  if (!row) return null;
  const income = row.transactionType === "income";
  const transfer = row.transactionType === "transfer";
  const Icon = transfer
    ? ArrowLeftRight
    : income
      ? ArrowDownLeft
      : ArrowUpRight;
  const tone = transfer
    ? "text-sky-600"
    : income
      ? "text-emerald-600"
      : "text-rose-600";
  const typeLabel = transfer ? "转账" : income ? "收入" : "支出";
  const title =
    row.merchant || (transfer ? "账户转账" : row.categoryName || "未命名账目");
  return (
    <Drawer
      open={open}
      onOpenChange={onOpenChange}
      direction={isMobile ? "bottom" : "right"}
    >
      <DrawerContent className="overflow-hidden p-0 data-[vaul-drawer-direction=bottom]:max-h-[min(88vh,720px)] data-[vaul-drawer-direction=right]:bottom-0 data-[vaul-drawer-direction=right]:left-auto data-[vaul-drawer-direction=right]:right-0 data-[vaul-drawer-direction=right]:top-0 data-[vaul-drawer-direction=right]:h-full data-[vaul-drawer-direction=right]:max-h-none data-[vaul-drawer-direction=right]:w-full data-[vaul-drawer-direction=right]:sm:w-[min(640px,calc(100vw-24px))] data-[vaul-drawer-direction=right]:sm:max-w-none">
        <DrawerHeader className="border-b border-border/60 px-4 pb-4 pt-3 text-left group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left sm:px-6">
          <div className="flex items-start gap-3">
            <span
              className={`mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-md bg-muted ${tone}`}
            >
              <Icon className="size-4" />
            </span>
            <div className="min-w-0">
              <DrawerTitle className="truncate text-base">
                {editing ? "编辑账目" : title}
              </DrawerTitle>
              <DrawerDescription className="mt-1">
                {editing
                  ? "修改后会立即重新计算账户余额和统计结果。"
                  : `${typeLabel} · ${formatDateTime(row.occurredAt)} · 账目 #${row.id}`}
              </DrawerDescription>
            </div>
          </div>
        </DrawerHeader>
        <ScrollArea className="min-h-0 flex-1">
          {editing ? (
            <TransactionEditForm
              row={row}
              accounts={accounts}
              categories={categories}
              onSubmit={onSubmit}
            />
          ) : (
            <div
              className="mx-auto w-full max-w-4xl cursor-default px-4 py-5 sm:px-6"
              onDoubleClick={() => onEdit(row)}
            >
              <div className="grid gap-x-10 gap-y-4 sm:grid-cols-2">
                <DetailField
                  label="金额"
                  value={
                    <span
                      className={`font-mono text-base font-semibold tabular-nums ${tone}`}
                    >
                      {transfer ? "" : income ? "+" : "-"}
                      {formatMoney(row.amount)} {row.currency}
                    </span>
                  }
                />
                <DetailField label="类型" value={typeLabel} />
                <DetailField
                  label="发生时间"
                  value={formatDateTime(row.occurredAt)}
                />
                <DetailField label="入账账户" value={row.accountName || "—"} />
                <DetailField
                  label="分类"
                  value={transfer ? "转账" : row.categoryName || "未分类"}
                />
                {transfer ? (
                  <DetailField
                    label="目标账户"
                    value={row.toAccountName || "—"}
                  />
                ) : null}
                <DetailField label="记录来源" value={row.source || "manual"} />
                {row.attachmentCount > 0 ? (
                  <DetailField
                    label="附件"
                    value={`${row.attachmentCount} 个`}
                  />
                ) : null}
              </div>
              {row.note ? (
                <section className="mt-6 border-t border-border/60 pt-4">
                  <h3 className="text-xs font-medium text-muted-foreground">
                    备注
                  </h3>
                  <p className="mt-2 whitespace-pre-wrap text-sm leading-6">
                    {row.note}
                  </p>
                </section>
              ) : null}
            </div>
          )}
        </ScrollArea>
        <DrawerFooter className="border-t border-border/60 bg-background/95 sm:flex-row sm:justify-end">
          {editing ? (
            <>
              <Button
                type="button"
                variant="ghost"
                className="gap-2"
                onClick={onCancelEdit}
                disabled={saving}
              >
                取消
              </Button>
              <Button
                type="submit"
                form="finance-transaction-edit"
                disabled={saving}
              >
                {saving ? "保存中" : "保存修改"}
              </Button>
            </>
          ) : (
            <>
              <Button
                type="button"
                variant="ghost"
                className="gap-2"
                onClick={() => void onArchive(row.id)}
              >
                <Archive className="size-4" />
                归档
              </Button>
              <Button
                type="button"
                variant="destructive"
                className="gap-2"
                onClick={() => onDelete(row)}
              >
                <Trash2 className="size-4" />
                删除
              </Button>
            </>
          )}
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}

function DetailField({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="min-w-0 border-b border-border/40 pb-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 truncate text-sm font-medium">{value}</div>
    </div>
  );
}

function FilterField({
  label,
  className = "",
  children,
}: {
  label: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <label className={`min-w-0 space-y-1.5 text-xs font-medium ${className}`}>
      <span className="block text-muted-foreground">{label}</span>
      {children}
    </label>
  );
}

function TransactionEditForm({
  row,
  accounts,
  categories,
  onSubmit,
}: {
  row: FinanceTransaction;
  accounts: FinanceAccount[];
  categories: FinanceCategory[];
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  const occurredAt = toDateTimeLocal(row.occurredAt);
  const [transactionType, setTransactionType] = useState<
    FinanceTransaction["transactionType"]
  >(row.transactionType);
  const [categoryId, setCategoryId] = useState(
    row.categoryId == null ? "" : String(row.categoryId),
  );
  const availableCategories = categories.filter(
    (item) =>
      !item.archived &&
      (item.direction === "both" || item.direction === transactionType),
  );
  function changeTransactionType(value: FinanceTransaction["transactionType"]) {
    setTransactionType(value);
    if (value === "transfer") {
      setCategoryId("");
      return;
    }
    const selectedCategory = categories.find(
      (item) => String(item.id) === categoryId,
    );
    if (
      selectedCategory &&
      selectedCategory.direction !== "both" &&
      selectedCategory.direction !== value
    ) {
      setCategoryId("");
    }
  }
  return (
    <form
      id="finance-transaction-edit"
      onSubmit={onSubmit}
      className="mx-auto grid w-full max-w-4xl gap-3 px-4 py-5 sm:grid-cols-2 sm:px-6"
    >
      <label className="space-y-1.5 text-xs font-medium">
        <span>类型</span>
        <select
          name="transactionType"
          value={transactionType}
          onChange={(event) =>
            changeTransactionType(
              event.target.value as FinanceTransaction["transactionType"],
            )
          }
          className="h-9 w-full rounded-md border border-input bg-background px-2 text-sm"
        >
          <option value="expense">支出</option>
          <option value="income">收入</option>
          <option value="transfer">转账</option>
        </select>
      </label>
      <label className="space-y-1.5 text-xs font-medium">
        <span>金额</span>
        <Input
          name="amount"
          type="number"
          min="0.01"
          step="0.01"
          defaultValue={row.amount}
          required
        />
      </label>
      <label className="space-y-1.5 text-xs font-medium">
        <span>发生时间</span>
        <Input
          name="occurredAt"
          type="datetime-local"
          defaultValue={occurredAt}
          required
        />
      </label>
      <label className="space-y-1.5 text-xs font-medium">
        <span>账户</span>
        <select
          name="accountId"
          defaultValue={row.accountId}
          className="h-9 w-full rounded-md border border-input bg-background px-2 text-sm"
          required
        >
          {accounts.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
      </label>
      <label className="space-y-1.5 text-xs font-medium">
        <span>目标账户（转账）</span>
        <select
          name="toAccountId"
          defaultValue={row.toAccountId ?? ""}
          className="h-9 w-full rounded-md border border-input bg-background px-2 text-sm"
        >
          <option value="">不设置</option>
          {accounts.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
      </label>
      <label className="space-y-1.5 text-xs font-medium">
        <span>分类</span>
        <select
          name="categoryId"
          value={categoryId}
          onChange={(event) => setCategoryId(event.target.value)}
          className="h-9 w-full rounded-md border border-input bg-background px-2 text-sm"
        >
          <option value="">不设置</option>
          {availableCategories.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
      </label>
      <label className="space-y-1.5 text-xs font-medium sm:col-span-2">
        <span>商户 / 来源</span>
        <Input
          name="merchant"
          defaultValue={row.merchant ?? ""}
          placeholder="可选"
        />
      </label>
      <label className="space-y-1.5 text-xs font-medium sm:col-span-2">
        <span>备注</span>
        <Input name="note" defaultValue={row.note ?? ""} placeholder="可选" />
      </label>
    </form>
  );
}

function toDateTimeLocal(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value.slice(0, 16);
  const pad = (part: number) => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
