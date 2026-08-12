import {
  ArrowRight,
  BookOpen,
  Camera,
  LayoutDashboard,
  List,
  RefreshCw,
} from "lucide-react";
import { Link } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { AccountsDrawer } from "./accounts-drawer";
import { ManualEntryDialog } from "./manual-entry-dialog";
import { FinanceSettingsDrawer } from "./settings-drawer";

const tabs = [
  {
    key: "overview",
    label: "Overview",
    href: "/finance/overview",
    icon: LayoutDashboard,
  },
  { key: "ledger", label: "消费分析", href: "/finance/ledger", icon: BookOpen },
  {
    key: "transactions",
    label: "账目",
    href: "/finance/transactions",
    icon: List,
  },
  {
    key: "ai-capture",
    label: "AI 入账",
    href: "/finance/ai-capture",
    icon: Camera,
  },
] as const;

export function FinancePageHeader({
  current,
  description,
  updatedAt,
  onRefresh,
  loading,
  onManualEntrySaved,
  onFinanceDataChanged,
}: {
  current: string;
  description: string;
  updatedAt?: string | null;
  onRefresh?: () => void;
  loading?: boolean;
  onManualEntrySaved?: () => Promise<void> | void;
  onFinanceDataChanged?: () => Promise<void> | void;
}) {
  return (
    <header className="border-b border-border/70 pb-4">
      <div className="flex min-w-0 flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div className="min-w-0">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className="inline-flex size-2 rounded-full bg-emerald-500" />
            <span>Finance</span>
            <span className="text-border">/</span>
            <span>个人财务</span>
            {updatedAt ? (
              <span className="hidden sm:inline">
                · 更新于 {formatUpdatedAt(updatedAt)}
              </span>
            ) : null}
          </div>
          <h1 className="mt-2 text-2xl font-semibold tracking-tight text-foreground">
            个人财务
          </h1>
          <p className="mt-1 max-w-2xl text-sm leading-6 text-muted-foreground">
            {description}
          </p>
        </div>
        <nav
          className="scrollbar-none flex max-w-full touch-pan-x items-center gap-1 overflow-x-auto rounded-lg bg-muted/50 p-1"
          aria-label="Finance navigation"
        >
          {tabs.map(({ key, label, href, icon: Icon }) => (
            <Button
              key={key}
              asChild
              variant={current === key ? "secondary" : "ghost"}
              size="sm"
              className="min-h-9 shrink-0 gap-1.5 px-2.5 sm:px-3"
            >
              <Link to={href}>
                <Icon className="size-3.5" />
                {label}
              </Link>
            </Button>
          ))}
          <AccountsDrawer onChanged={onFinanceDataChanged} />
          <FinanceSettingsDrawer />
          <ManualEntryDialog onSaved={onManualEntrySaved} />
          {onRefresh ? (
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              title="刷新数据"
              aria-label="刷新数据"
              onClick={onRefresh}
              disabled={loading}
            >
              <RefreshCw
                className={loading ? "size-3.5 animate-spin" : "size-3.5"}
              />
            </Button>
          ) : null}
        </nav>
      </div>
      {current === "ledger" ? (
        <div className="mt-4 flex items-center gap-2 text-xs text-muted-foreground">
          <span className="font-medium text-foreground">主要入口</span>
          <Link
            to="/finance/ai-capture"
            className="inline-flex items-center gap-1 text-primary hover:underline"
          >
            AI 图片记账 <ArrowRight className="size-3" />
          </Link>
        </div>
      ) : null}
    </header>
  );
}

function formatUpdatedAt(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString("zh-CN", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      });
}
