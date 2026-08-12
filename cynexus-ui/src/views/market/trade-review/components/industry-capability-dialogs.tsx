import { Activity, Landmark } from "lucide-react";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { IndustryFundFlowTable } from "@/views/industry-application/components/industry-fund-flow-table";
import { IndustryTdxFundPanel } from "@/views/industry-application/components/industry-tdx-fund-panel";
import type { IndustryApplicationTargetCode } from "@/views/industry-application/lib/types";

interface IndustryCapabilityDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tradeDate: string;
}

export function IndustryCapitalDialog({
  open,
  onOpenChange,
  target,
  tradeDate,
}: IndustryCapabilityDialogProps & {
  target: IndustryApplicationTargetCode | null;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="grid h-[92vh] w-[96vw] max-w-[96vw] grid-rows-[auto_minmax(0,1fr)] gap-0 overflow-hidden p-0 sm:w-[96vw] sm:max-w-[96vw]"
        aria-describedby="trade-review-industry-capital-description"
      >
        <DialogHeader className="border-b border-border/60 px-5 py-4 pr-14">
          <DialogTitle className="flex items-center gap-2 text-base">
            <Landmark className="size-4 text-muted-foreground" />
            Industry / Concept Application · Capital
          </DialogTitle>
          <DialogDescription id="trade-review-industry-capital-description" className="text-xs">
            {target
              ? `${target.name} · ${target.code} · 数据截止 ${tradeDate}`
              : `当前尚未关联行业 · 复盘日期 ${tradeDate}`}
          </DialogDescription>
        </DialogHeader>
        <div className="min-h-0 overflow-hidden bg-background p-4">
          {open ? (
            <IndustryTdxFundPanel
              tradeDate={tradeDate}
              target={
                target
                  ? {
                      target_type: target.kind,
                      symbol: target.code,
                      name: target.name,
                    }
                  : null
              }
            />
          ) : null}
        </div>
      </DialogContent>
    </Dialog>
  );
}

export function IndustryFundFlowDialog({
  open,
  onOpenChange,
  tradeDate,
}: IndustryCapabilityDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="grid h-[92vh] w-[96vw] max-w-[96vw] grid-rows-[auto_minmax(0,1fr)] gap-0 overflow-hidden p-0 sm:w-[96vw] sm:max-w-[96vw]"
        aria-describedby="trade-review-industry-fund-flow-description"
      >
        <DialogHeader className="border-b border-border/60 px-5 py-4 pr-14">
          <DialogTitle className="flex items-center gap-2 text-base">
            <Activity className="size-4 text-muted-foreground" />
            Industry / Concept Application · Fund Flow
          </DialogTitle>
          <DialogDescription id="trade-review-industry-fund-flow-description" className="text-xs">
            保持原行业资金流页面布局，数据截止复盘交易日 {tradeDate}。
          </DialogDescription>
        </DialogHeader>
        <div className="min-h-0 overflow-hidden bg-background p-4">
          {open ? <IndustryFundFlowTable tradeDate={tradeDate} /> : null}
        </div>
      </DialogContent>
    </Dialog>
  );
}
