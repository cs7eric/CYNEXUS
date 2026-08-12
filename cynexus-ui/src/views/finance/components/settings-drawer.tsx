import { useCallback, useState } from "react";
import { ExternalLink, Plus, Settings2, Tag, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from "@/components/ui/drawer";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { notification } from "@/components/ui/notification";
import { useIsMobile } from "@/hooks/use-mobile";
import {
  archiveFinanceCategory,
  fetchFinanceCategories,
  saveFinanceCategory,
  type FinanceCategory,
} from "@/services/finance/ledger";

export function FinanceSettingsDrawer() {
  const isMobile = useIsMobile();
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState<FinanceCategory[]>([]);
  const [category, setCategory] = useState({ name: "", direction: "expense" });
  const [categoryToArchive, setCategoryToArchive] =
    useState<FinanceCategory | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setCategories(await fetchFinanceCategories(true));
    } catch (cause) {
      notification.danger({
        title: "设置加载失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    } finally {
      setLoading(false);
    }
  }, []);

  function handleOpenChange(nextOpen: boolean) {
    setOpen(nextOpen);
    if (nextOpen) void load();
  }

  async function addCategory(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await saveFinanceCategory({
        name: category.name.trim(),
        direction: category.direction as FinanceCategory["direction"],
      });
      setCategory({ name: "", direction: "expense" });
      notification.success({ title: "分类已创建" });
      await load();
    } catch (cause) {
      notification.danger({
        title: "分类创建失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    }
  }

  async function removeCategory() {
    if (!categoryToArchive) return;
    try {
      await archiveFinanceCategory(categoryToArchive.id);
      setCategoryToArchive(null);
      notification.success({ title: "分类已归档" });
      await load();
    } catch (cause) {
      notification.danger({
        title: "分类归档失败",
        description: cause instanceof Error ? cause.message : String(cause),
      });
    }
  }

  const activeCategories = categories.filter((item) => !item.archived);

  return (
    <Drawer
      open={open}
      onOpenChange={handleOpenChange}
      direction={isMobile ? "bottom" : "right"}
    >
      <DrawerTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="min-h-9 shrink-0 gap-1.5 px-2.5 sm:px-3"
        >
          <Settings2 className="size-3.5" />
          设置
        </Button>
      </DrawerTrigger>
      <DrawerContent className="data-[vaul-drawer-direction=right]:w-full data-[vaul-drawer-direction=right]:sm:max-w-xl data-[vaul-drawer-direction=bottom]:max-h-[88vh]">
        <DrawerHeader className="border-b border-border/60">
          <DrawerTitle>Finance 设置</DrawerTitle>
          <DrawerDescription>
            管理记账分类、统计口径和常用的账本入口。
          </DrawerDescription>
        </DrawerHeader>
        <ScrollArea className="min-h-0 flex-1">
          <div className="px-4 sm:px-6">
            <section className="border-b border-border/60 py-5">
              <div className="flex items-start gap-3">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted text-foreground">
                  <Tag className="size-4" />
                </span>
                <div>
                  <h2 className="text-sm font-semibold">记账分类</h2>
                  <p className="mt-0.5 text-xs leading-5 text-muted-foreground">
                    管理手动记账和账单识别时使用的收支分类。
                  </p>
                </div>
              </div>
              <form
                onSubmit={addCategory}
                className="mt-4 grid gap-2 sm:grid-cols-[minmax(0,1fr)_112px_auto]"
              >
                <Input
                  required
                  value={category.name}
                  onChange={(event) =>
                    setCategory({ ...category, name: event.target.value })
                  }
                  placeholder="分类名称，例如：宠物"
                />
                <select
                  value={category.direction}
                  onChange={(event) =>
                    setCategory({ ...category, direction: event.target.value })
                  }
                  className="h-9 rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-1 focus-visible:ring-ring"
                >
                  <option value="expense">支出</option>
                  <option value="income">收入</option>
                  <option value="both">收支</option>
                </select>
                <Button type="submit" variant="outline" className="gap-1.5">
                  <Plus className="size-3.5" />
                  添加
                </Button>
              </form>
              {loading ? (
                <div className="mt-4 h-20 animate-pulse rounded-md bg-muted/50" />
              ) : activeCategories.length ? (
                <div className="mt-4 divide-y divide-border/60 border-y border-border/60">
                  {activeCategories.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center justify-between gap-3 py-2.5 text-sm"
                    >
                      <div className="min-w-0 truncate">
                        {item.name}
                        <span className="ml-2 text-xs text-muted-foreground">
                          {item.direction === "income"
                            ? "收入"
                            : item.direction === "expense"
                              ? "支出"
                              : "收支"}
                        </span>
                      </div>
                      {item.system ? (
                        <span className="shrink-0 text-[11px] text-muted-foreground">
                          系统
                        </span>
                      ) : (
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon-sm"
                          title={`归档分类 ${item.name}`}
                          aria-label={`归档分类 ${item.name}`}
                          onClick={() => setCategoryToArchive(item)}
                        >
                          <Trash2 className="size-3.5 text-muted-foreground" />
                        </Button>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="mt-4 text-xs text-muted-foreground">
                  暂无可用分类。
                </p>
              )}
            </section>
            <section className="border-b border-border/60 py-5">
              <div className="flex items-start gap-3">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted text-foreground">
                  <Settings2 className="size-4" />
                </span>
                <div>
                  <h2 className="text-sm font-semibold">统计规则</h2>
                  <p className="mt-0.5 text-xs leading-5 text-muted-foreground">
                    当前账本使用的固定口径，确保不同页面的数字一致。
                  </p>
                </div>
              </div>
              <div className="mt-4 divide-y divide-border/60 border-y border-border/60">
                <SettingRow
                  label="账户转账"
                  detail="只更新账户余额，不计入收入或支出。"
                />
                <SettingRow
                  label="信用账户还款"
                  detail="资产账户转入信用账户，按转账处理。"
                />
                <SettingRow
                  label="统计范围"
                  detail="只统计已确认且未归档的账目。"
                />
              </div>
            </section>
            <section className="py-5">
              <div className="flex items-start gap-3">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted text-foreground">
                  <ExternalLink className="size-4" />
                </span>
                <div>
                  <h2 className="text-sm font-semibold">常用入口</h2>
                  <p className="mt-0.5 text-xs leading-5 text-muted-foreground">
                    继续处理完整账目或 AI 识别结果。
                  </p>
                </div>
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <Button asChild variant="outline" size="sm">
                  <Link to="/finance/transactions">查看账目</Link>
                </Button>
                <Button asChild variant="outline" size="sm">
                  <Link to="/finance/ai-capture">AI 入账</Link>
                </Button>
              </div>
            </section>
          </div>
        </ScrollArea>
      </DrawerContent>
      <ConfirmDialog
        open={categoryToArchive != null}
        onOpenChange={(nextOpen) => {
          if (!nextOpen) setCategoryToArchive(null);
        }}
        title="归档这个分类？"
        description="历史账目仍会保留，但新账目将不再默认显示这个分类。"
        confirmText="归档分类"
        destructive
        onConfirm={removeCategory}
      />
    </Drawer>
  );
}

function SettingRow({ label, detail }: { label: string; detail: string }) {
  return (
    <div className="flex items-start justify-between gap-4 py-3">
      <span className="text-sm font-medium">{label}</span>
      <span className="max-w-[18rem] text-right text-xs leading-5 text-muted-foreground">
        {detail}
      </span>
    </div>
  );
}
