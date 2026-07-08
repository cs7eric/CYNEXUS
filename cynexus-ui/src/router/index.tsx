import { createBrowserRouter } from "react-router-dom"
import App from "../App"
import ApplicationAnalysisPage from "../views/application-analysis"
import DashboardPage from "../views/dashboard"
import DownloaderPage from "../views/downloader"
import HomePage from "../views/home"
import HeatmapDemoPage, { HeatmapDataDebug } from "../views/heatmap-demo"
import IndustryApplicationPage from "../views/industry-application"
import Mp4ToWordPage from "../views/mp4-to-word"
import Mp4HistoryPage from "../views/mp4-to-word/history"
import SchedulerSettingsPage from "../views/settings/scheduler"
import AiProviderSettingsPage from "../views/settings/ai-provider"
import DataSourceSettingsPage from "../views/settings/data-source"
import PromptSettingsPage from "../views/settings/prompt"
import SelfSelectedPage from "../views/self-selected"
import StockOverviewPage from "../views/stock-overview"
import StockOverviewMarketPulsePage from "../views/stock-overview/market-pulse"
import MarketPulsePage from "../views/market/market-pulse"
import MarketSentimentPage from "../views/market/market-sentiment"
import MarketMarginTradingPage from "../views/market/market-margin-trading"
import PocPage from "../views/poc"
import AiPocPage from "../views/poc/ai"
import MarkdownRenderPocPage from "../views/poc/markdown-render"
import StockReviewPage from "../views/stock-review"
import KnowledgeBasePage from "../views/knowledge-base"
import KnowledgeBaseWritingPage from "../views/knowledge-base/writing"
import KnowledgeBaseViewPage from "../views/knowledge-base/view"

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
        path: "stock-overview",
        element: <StockOverviewPage />,
      },
      {
        path: "stock-overview/market",
        element: <StockOverviewMarketPulsePage />,
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
        path: "poc",
        element: <PocPage />,
      },
      {
        path: "poc/ai",
        element: <AiPocPage />,
      },
      {
        path: "poc/markdown-render",
        element: <MarkdownRenderPocPage />,
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
        path: "settings/ai-provider",
        element: <AiProviderSettingsPage />,
      },
      {
        path: "settings/data-source",
        element: <DataSourceSettingsPage />,
      },
      {
        path: "settings/prompt",
        element: <PromptSettingsPage />,
      },
    ],
  },
])
