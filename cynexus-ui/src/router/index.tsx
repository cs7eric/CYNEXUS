/* eslint-disable react-refresh/only-export-components */
import { createBrowserRouter, Navigate, useParams, useSearchParams } from "react-router-dom"
import { lazy, Suspense, type ComponentType } from "react"
import App from "../App"
import { mobileRoutes } from "@/mobile/routes"

const ApplicationAnalysisPage = lazy(() => import("../views/application-analysis"))
const DashboardPage = lazy(() => import("../views/dashboard"))
const DownloaderPage = lazy(() => import("../views/downloader"))
const HomePage = lazy(() => import("../views/home"))
const HeatmapDemoPage = lazy(() => import("../views/heatmap-demo"))
const HeatmapDataDebug = lazy(() => import("../views/heatmap-demo").then(({ HeatmapDataDebug }) => ({ default: HeatmapDataDebug })))
const IndustryApplicationPage = lazy(() => import("../views/industry-application"))
const Mp4ToWordPage = lazy(() => import("../views/mp4-to-word"))
const Mp4HistoryPage = lazy(() => import("../views/mp4-to-word/history"))
const Mp4ProcessHistoryPage = lazy(() => import("../views/mp4-to-word/process-history"))
const SchedulerSettingsPage = lazy(() => import("../views/settings/scheduler"))
const WechatHistoryPage = lazy(() => import("../views/settings/wechat-history"))
const ApiWorkflowsPage = lazy(() => import("../views/settings/api-workflows"))
const AiProviderSettingsPage = lazy(() => import("../views/ai/provider"))
const DataSourceSettingsPage = lazy(() => import("../views/settings/data-source"))
const ProviderSettingsPage = lazy(() => import("../views/settings/provider"))
const PromptSettingsPage = lazy(() => import("../views/ai/prompt"))
const AiUsageSettingsPage = lazy(() => import("../views/ai/usage"))
const AutonomousAgentPage = lazy(() => import("../views/cynexus-agent"))
const SelfSelectedPage = lazy(() => import("../views/self-selected"))
const StockOverviewPage = lazy(() => import("../views/stock-overview"))
const StockOverviewMarketPulsePage = lazy(() => import("../views/stock-overview/market-pulse"))
const MarketPulsePage = lazy(() => import("../views/market/market-pulse"))
const MarketSentimentPage = lazy(() => import("../views/market/market-sentiment"))
const MarketMarginTradingPage = lazy(() => import("../views/market/market-margin-trading"))
const MarketEtfPage = lazy(() => import("../views/market/market-etf"))
const MarketIndicatorPage = lazy(() => import("../views/data-collector/market-indicator"))
const LimitBoardMonitorPage = lazy(() => import("../views/data-collector/limit-board-monitor"))
const MarketFuturesPositionPage = lazy(() => import("../views/market/market-futures-position"))
const MarketOverviewPage = lazy(() => import("../views/market/market-overview"))
const MarketInformationPage = lazy(() => import("../views/information"))
const MarketInformationAnalyzedPage = lazy(() => import("../views/information").then(({ MarketInformationAnalyzedPage }) => ({ default: MarketInformationAnalyzedPage })))
const SocialMonitoringPage = lazy(() => import("../views/information/social-monitoring"))
const PocPage = lazy(() => import("../views/poc"))
const AStockSkillPocPage = lazy(() => import("../views/poc/a-stock-skill/page"))
const AiPocPage = lazy(() => import("../views/poc/ai"))
const MarkdownRenderPocPage = lazy(() => import("../views/poc/markdown-render"))
const EasyTdxPocPage = lazy(() => import("../views/poc/easy-tdx/page"))
const ShadcnPocPage = lazy(() => import("../views/poc/shadcn"))
const StockReviewPage = lazy(() => import("../views/stock-review"))
const KnowledgeBasePage = lazy(() => import("../views/knowledge-base"))
const KnowledgeBaseWritingPage = lazy(() => import("../views/knowledge-base/writing"))
const KnowledgeBaseViewPage = lazy(() => import("../views/knowledge-base/view"))
const FinanceOverviewPage = lazy(() => import("../views/finance/overview"))
const FinanceLedgerPage = lazy(() => import("../views/finance/ledger"))
const FinanceTransactionsPage = lazy(() => import("../views/finance/transactions"))
const FinanceAiCapturePage = lazy(() => import("../views/finance/ai-capture"))
const PersonalPortfolioPage = lazy(() => import("../views/market/personal-portfolio"))
const PersonalPortfolioImportReviewPage = lazy(() => import("../views/market/personal-portfolio/import-review"))
const PersonalPortfolioReviewEditorPage = lazy(() => import("../views/market/personal-portfolio/review-editor"))
const PersonalPortfolioStockDetailPage = lazy(() => import("../views/market/personal-portfolio/stock-detail"))
const TradeReviewPage = lazy(() => import("../views/market/trade-review"))
const CynexusScreenerPage = lazy(() => import("../views/cynexus-screener"))

const RouteLoading = () => (
  <div className="flex min-h-[40vh] items-center justify-center text-sm text-muted-foreground">
    Loading…
  </div>
)

function pageElement(Page: ComponentType) {
  return (
    <Suspense fallback={<RouteLoading />}>
      <Page />
    </Suspense>
  )
}

function LegacyPortfolioImportRoute() {
  const { taskId } = useParams()
  return <Navigate to={`/personal-portfolio/imports/${taskId}/review`} replace />
}

function LegacyPortfolioNewReviewRoute() {
  const [searchParams] = useSearchParams()
  const next = new URLSearchParams(searchParams)
  next.set("review", "new")
  return <Navigate to={`/personal-portfolio?${next.toString()}`} replace />
}

export const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      ...mobileRoutes,
      {
        index: true,
        element: pageElement(HomePage),
      },
      {
        path: "downloader",
        element: pageElement(DownloaderPage),
      },
      {
        path: "dashboard",
        element: pageElement(DashboardPage),
      },
      {
        path: "finance/overview",
        element: pageElement(FinanceOverviewPage),
      },
      {
        path: "finance/ledger",
        element: pageElement(FinanceLedgerPage),
      },
      {
        path: "finance/transactions",
        element: pageElement(FinanceTransactionsPage),
      },
      {
        path: "finance/ai-capture",
        element: pageElement(FinanceAiCapturePage),
      },
      {
        path: "mp4-to-word",
        element: pageElement(Mp4ToWordPage),
      },
      {
        path: "mp4-to-word/history",
        element: pageElement(Mp4HistoryPage),
      },
      {
        path: "mp4-to-word/history/:id",
        element: pageElement(Mp4HistoryPage),
      },
      {
        path: "mp4-to-word/process-history",
        element: pageElement(Mp4ProcessHistoryPage),
      },
      {
        path: "stock-overview",
        element: pageElement(StockOverviewPage),
      },
      {
        path: "stock-overview/market",
        element: pageElement(StockOverviewMarketPulsePage),
      },
      {
        path: "market/overview",
        element: pageElement(MarketOverviewPage),
      },
      {
        path: "market/pulse",
        element: pageElement(MarketPulsePage),
      },
      {
        path: "market/sentiment",
        element: pageElement(MarketSentimentPage),
      },
      {
        path: "market/margin-trading",
        element: pageElement(MarketMarginTradingPage),
      },
      {
        path: "market/etf",
        element: pageElement(MarketEtfPage),
      },
      {
        path: "market/indicator",
        element: <Navigate to="/data-collector/market-indicator" replace />,
      },
      {
        path: "data-collector",
        element: <Navigate to="/data-collector/market-indicator" replace />,
      },
      {
        path: "data-collector/market-indicator",
        element: pageElement(MarketIndicatorPage),
      },
      {
        path: "data-collector/limit-board-monitor",
        element: pageElement(LimitBoardMonitorPage),
      },
      {
        path: "market/futures-position",
        element: pageElement(MarketFuturesPositionPage),
      },
      {
        path: "personal-portfolio",
        element: pageElement(PersonalPortfolioPage),
      },
      {
        path: "personal-portfolio/imports",
        element: <Navigate to="/personal-portfolio?imports=1" replace />,
      },
      {
        path: "personal-portfolio/imports/:taskId/review",
        element: pageElement(PersonalPortfolioImportReviewPage),
      },
      {
        path: "personal-portfolio/imports/:taskId",
        element: <LegacyPortfolioImportRoute />,
      },
      {
        path: "personal-portfolio/reviews/new",
        element: <LegacyPortfolioNewReviewRoute />,
      },
      {
        path: "personal-portfolio/reviews/:reviewId",
        element: pageElement(PersonalPortfolioReviewEditorPage),
      },
      {
        path: "personal-portfolio/stocks/:symbol",
        element: pageElement(PersonalPortfolioStockDetailPage),
      },
      {
        path: "trade-review",
        element: pageElement(TradeReviewPage),
      },
      {
        path: "cynexus-screener",
        element: pageElement(CynexusScreenerPage),
      },
      {
        path: "information",
        element: pageElement(MarketInformationPage),
      },
      {
        path: "information/analyzed",
        element: pageElement(MarketInformationAnalyzedPage),
      },
      {
        path: "information/social-monitoring",
        element: pageElement(SocialMonitoringPage),
      },
      {
        path: "market/information",
        element: <Navigate to="/information" replace />,
      },
      {
        path: "poc",
        element: pageElement(PocPage),
      },
      {
        path: "poc/ai",
        element: pageElement(AiPocPage),
      },
      {
        path: "poc/a-stock-skill",
        element: pageElement(AStockSkillPocPage),
      },
      {
        path: "poc/markdown-render",
        element: pageElement(MarkdownRenderPocPage),
      },
      {
        path: "poc/easy-tdx",
        element: pageElement(EasyTdxPocPage),
      },
      {
        path: "poc/shadcn",
        element: pageElement(ShadcnPocPage),
      },
      {
        path: "knowledge-base",
        element: pageElement(KnowledgeBasePage),
      },
      {
        path: "knowledge-base/writing",
        element: pageElement(KnowledgeBaseWritingPage),
      },
      {
        path: "knowledge-base/view",
        element: pageElement(KnowledgeBaseViewPage),
      },
      {
        path: "stock-overview/application-analysis",
        element: pageElement(ApplicationAnalysisPage),
      },
      {
        path: "stock-overview/industry-application",
        element: pageElement(IndustryApplicationPage),
      },
      {
        path: "heatmap-demo",
        element: pageElement(HeatmapDemoPage),
      },
      {
        path: "heatmap-data-debug",
        element: pageElement(HeatmapDataDebug),
      },
      {
        path: "stock-overview/self-selected",
        element: pageElement(SelfSelectedPage),
      },
      {
        path: "stock-review",
        element: pageElement(StockReviewPage),
      },
      {
        path: "settings/scheduler",
        element: pageElement(SchedulerSettingsPage),
      },
      {
        path: "settings/wechat-history",
        element: pageElement(WechatHistoryPage),
      },
      {
        path: "settings/api-workflows",
        element: pageElement(ApiWorkflowsPage),
      },
      {
        path: "ai",
        element: <Navigate to="/ai/provider" replace />,
      },
      {
        path: "cynexus-agent",
        element: pageElement(AutonomousAgentPage),
      },
      {
        path: "ai/provider",
        element: pageElement(AiProviderSettingsPage),
      },
      {
        path: "ai/agent",
        element: <Navigate to="/cynexus-agent" replace />,
      },
      {
        path: "settings/data-source",
        element: pageElement(DataSourceSettingsPage),
      },
      {
        path: "settings/provider",
        element: pageElement(ProviderSettingsPage),
      },
      {
        path: "ai/prompt",
        element: pageElement(PromptSettingsPage),
      },
      {
        path: "ai/usage",
        element: pageElement(AiUsageSettingsPage),
      },
      {
        path: "settings/ai-provider",
        element: <Navigate to="/ai/provider" replace />,
      },
      {
        path: "settings/prompt",
        element: <Navigate to="/ai/prompt" replace />,
      },
      {
        path: "settings/ai-usage",
        element: <Navigate to="/ai/usage" replace />,
      },
    ],
  },
])
