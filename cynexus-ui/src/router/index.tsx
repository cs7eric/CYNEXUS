import { createBrowserRouter, Navigate, useParams } from "react-router-dom"
import App from "../App"
import ApplicationAnalysisPage from "../views/application-analysis"
import DashboardPage from "../views/dashboard"
import DownloaderPage from "../views/downloader"
import HomePage from "../views/home"
import HeatmapDemoPage, { HeatmapDataDebug } from "../views/heatmap-demo"
import IndustryApplicationPage from "../views/industry-application"
import Mp4ToWordPage from "../views/mp4-to-word"
import Mp4HistoryPage from "../views/mp4-to-word/history"
import Mp4ProcessHistoryPage from "../views/mp4-to-word/process-history"
import SchedulerSettingsPage from "../views/settings/scheduler"
import AiProviderSettingsPage from "../views/ai/provider"
import DataSourceSettingsPage from "../views/settings/data-source"
import ProviderSettingsPage from "../views/settings/provider"
import PromptSettingsPage from "../views/ai/prompt"
import AiUsageSettingsPage from "../views/ai/usage"
import AutonomousAgentPage from "../views/cynexus-agent"
import SelfSelectedPage from "../views/self-selected"
import StockOverviewPage from "../views/stock-overview"
import StockOverviewMarketPulsePage from "../views/stock-overview/market-pulse"
import MarketPulsePage from "../views/market/market-pulse"
import MarketSentimentPage from "../views/market/market-sentiment"
import MarketMarginTradingPage from "../views/market/market-margin-trading"
import MarketEtfPage from "../views/market/market-etf"
import MarketIndicatorPage from "../views/market/market-indicator"
import MarketFuturesPositionPage from "../views/market/market-futures-position"
import MarketOverviewPage from "../views/market/market-overview"
import MarketInformationPage, { MarketInformationAnalyzedPage } from "../views/information"
import PocPage from "../views/poc"
import AStockSkillPocPage from "../views/poc/a-stock-skill/page"
import AiPocPage from "../views/poc/ai"
import MarkdownRenderPocPage from "../views/poc/markdown-render"
import EasyTdxPocPage from "../views/poc/easy-tdx/page"
import ShadcnPocPage from "../views/poc/shadcn"
import StockReviewPage from "../views/stock-review"
import KnowledgeBasePage from "../views/knowledge-base"
import KnowledgeBaseWritingPage from "../views/knowledge-base/writing"
import KnowledgeBaseViewPage from "../views/knowledge-base/view"
import FinanceOverviewPage from "../views/finance/overview"
import FinanceLedgerPage from "../views/finance/ledger"
import FinanceTransactionsPage from "../views/finance/transactions"
import FinanceAccountsPage from "../views/finance/accounts"
import FinanceAiCapturePage from "../views/finance/ai-capture"
import PersonalPortfolioPage from "../views/market/personal-portfolio"
import PersonalPortfolioImportReviewPage from "../views/market/personal-portfolio/import-review"
import PersonalPortfolioReviewEditorPage from "../views/market/personal-portfolio/review-editor"
import PersonalPortfolioStockDetailPage from "../views/market/personal-portfolio/stock-detail"

function LegacyPortfolioImportRoute() {
  const { taskId } = useParams()
  return <Navigate to={`/personal-portfolio/imports/${taskId}/review`} replace />
}

export const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: "downloader",
        element: <DownloaderPage />,
      },
      {
        path: "dashboard",
        element: <DashboardPage />,
      },
      {
        path: "finance/overview",
        element: <FinanceOverviewPage />,
      },
      {
        path: "finance/ledger",
        element: <FinanceLedgerPage />,
      },
      {
        path: "finance/transactions",
        element: <FinanceTransactionsPage />,
      },
      {
        path: "finance/accounts",
        element: <FinanceAccountsPage />,
      },
      {
        path: "finance/ai-capture",
        element: <FinanceAiCapturePage />,
      },
      {
        path: "mp4-to-word",
        element: <Mp4ToWordPage />,
      },
      {
        path: "mp4-to-word/history",
        element: <Mp4HistoryPage />,
      },
      {
        path: "mp4-to-word/history/:id",
        element: <Mp4HistoryPage />,
      },
      {
        path: "mp4-to-word/process-history",
        element: <Mp4ProcessHistoryPage />,
      },
      {
        path: "stock-overview",
        element: <StockOverviewPage />,
      },
      {
        path: "stock-overview/market",
        element: <StockOverviewMarketPulsePage />,
      },
      {
        path: "market/overview",
        element: <MarketOverviewPage />,
      },
      {
        path: "market/pulse",
        element: <MarketPulsePage />,
      },
      {
        path: "market/sentiment",
        element: <MarketSentimentPage />,
      },
      {
        path: "market/margin-trading",
        element: <MarketMarginTradingPage />,
      },
      {
        path: "market/etf",
        element: <MarketEtfPage />,
      },
      {
        path: "market/indicator",
        element: <MarketIndicatorPage />,
      },
      {
        path: "market/futures-position",
        element: <MarketFuturesPositionPage />,
      },
      {
        path: "personal-portfolio",
        element: <PersonalPortfolioPage />,
      },
      {
        path: "personal-portfolio/imports",
        element: <Navigate to="/personal-portfolio?imports=1" replace />,
      },
      {
        path: "personal-portfolio/imports/:taskId/review",
        element: <PersonalPortfolioImportReviewPage />,
      },
      {
        path: "personal-portfolio/imports/:taskId",
        element: <LegacyPortfolioImportRoute />,
      },
      {
        path: "personal-portfolio/reviews/:reviewId",
        element: <PersonalPortfolioReviewEditorPage />,
      },
      {
        path: "personal-portfolio/stocks/:symbol",
        element: <PersonalPortfolioStockDetailPage />,
      },
      {
        path: "information",
        element: <MarketInformationPage />,
      },
      {
        path: "information/analyzed",
        element: <MarketInformationAnalyzedPage />,
      },
      {
        path: "market/information",
        element: <Navigate to="/information" replace />,
      },
      {
        path: "poc",
        element: <PocPage />,
      },
      {
        path: "poc/ai",
        element: <AiPocPage />,
      },
      {
        path: "poc/a-stock-skill",
        element: <AStockSkillPocPage />,
      },
      {
        path: "poc/markdown-render",
        element: <MarkdownRenderPocPage />,
      },
      {
        path: "poc/easy-tdx",
        element: <EasyTdxPocPage />,
      },
      {
        path: "poc/shadcn",
        element: <ShadcnPocPage />,
      },
      {
        path: "knowledge-base",
        element: <KnowledgeBasePage />,
      },
      {
        path: "knowledge-base/writing",
        element: <KnowledgeBaseWritingPage />,
      },
      {
        path: "knowledge-base/view",
        element: <KnowledgeBaseViewPage />,
      },
      {
        path: "stock-overview/application-analysis",
        element: <ApplicationAnalysisPage />,
      },
      {
        path: "stock-overview/industry-application",
        element: <IndustryApplicationPage />,
      },
      {
        path: "heatmap-demo",
        element: <HeatmapDemoPage />,
      },
      {
        path: "heatmap-data-debug",
        element: <HeatmapDataDebug />,
      },
      {
        path: "stock-overview/self-selected",
        element: <SelfSelectedPage />,
      },
      {
        path: "stock-review",
        element: <StockReviewPage />,
      },
      {
        path: "settings/scheduler",
        element: <SchedulerSettingsPage />,
      },
      {
        path: "ai",
        element: <Navigate to="/ai/provider" replace />,
      },
      {
        path: "cynexus-agent",
        element: <AutonomousAgentPage />,
      },
      {
        path: "ai/provider",
        element: <AiProviderSettingsPage />,
      },
      {
        path: "ai/agent",
        element: <Navigate to="/cynexus-agent" replace />,
      },
      {
        path: "settings/data-source",
        element: <DataSourceSettingsPage />,
      },
      {
        path: "settings/provider",
        element: <ProviderSettingsPage />,
      },
      {
        path: "ai/prompt",
        element: <PromptSettingsPage />,
      },
      {
        path: "ai/usage",
        element: <AiUsageSettingsPage />,
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
