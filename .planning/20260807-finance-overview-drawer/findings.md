# Findings

## Existing structure

- `FinancePageHeader` 当前把 Overview、消费分析、账目、账户、AI 入账作为路由按钮，并由 `ManualEntryDialog` 提供手动入账按钮。
- `cynexus-ui/src/views/finance/accounts/index.tsx` 目前承载账户列表、新增账户、记账分类、信用还款和分期面板。
- `account-drawer.tsx` 已存在，但只在 Accounts 页面点击账户时使用；`AccountSnapshot` 也有 Overview 内部账户 Drawer。
- `CreditAccountActions`、`CreditRepaymentForm`、`CreditInstallmentPanel` 已可复用，信用还款使用 transfer，不计入支出。
- 账户、分类 API 已集中在 `src/services/finance/ledger.ts`：账户、分类、分期、交易均已有读写方法。

## Target UX

- Overview 顶部导航保留页面路由入口，账户改为 Drawer 入口，设置改为 Drawer 入口。
- 账户 Drawer 展示账户列表；每个账户是一行 Collapsible，展开后展示余额/额度/账单日等详情。
- 信用账户展开后提供还款和分期列表入口，避免在 Overview 主页面占用大块空间。
- 设置 Drawer 至少承载记账分类，并保留“新增分类”和归档分类能力；后续设置项可继续放入同一 Drawer。
- 导航按钮应统一使用同一套 `ghost/secondary` 尺寸与间距；手动入账按钮不再使用独立的深色主按钮样式。

## Current navigation constraints

- `FinancePageHeader` is shared by all Finance routes, so account/settings Drawers can be mounted there and remain available while the user stays on the current page.
- `/finance/accounts` and its sidebar entry currently exist as a full page; the requested change should remove the user-facing navigation entry while keeping the route temporarily safe for existing links.
- `src/components/ui/accordion.tsx` provides the Radix-backed Collapsible-like Accordion primitives and should be reused for account rows.
- Overview currently renders `AccountSnapshot` as a large content section; it should be removed from the main content area once the header Account Drawer becomes the canonical account surface.

## Implemented so far

- Added `accounts-drawer.tsx`: shared Finance header trigger, account loading/error/empty states, Accordion-based account rows, and credit repayment/installment dialogs.
- Added `settings-drawer.tsx`: category management, accounting rules, and common Finance links.
- Removed the Accounts route/sidebar entry and removed Overview's inline AccountSnapshot block.
- Made Manual Entry use the same ghost navigation-button treatment as the other header actions.
- Removed the Ledger page's inline AccountSnapshot as well; account access is now centralized in the shared header Drawer.
