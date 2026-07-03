// Deprecated compatibility barrel.
// New code should import from src/services/* directly.

export { fetchWithRetry } from "@/services/common/http"
export { SERVICE_PREFIX } from "@/services/common/service-prefix"

export type {
  AiBindingItem,
  AiCapability,
  AiProviderItem,
  AiProviderType,
} from "@/services/ai/provider"
export {
  createAiProvider,
  deleteAiProvider,
  fetchAiBindings,
  fetchAiCapabilities,
  fetchAiProviderTypes,
  fetchAiProviders,
  updateAiProvider,
  upsertAiBinding,
} from "@/services/ai/provider"

export type {
  ApplicationAnalysisDailySnapshotFile,
  ApplicationAnalysisDailySnapshotResponse,
  ApplicationAnalysisRecent30FullItem,
  ApplicationAnalysisRecent30FullResponse,
  ApplicationAnalysisResultFile,
  ApplicationAnalysisSchedulerStatus,
  ApplicationAnalysisTarget,
} from "@/services/ai/application-analysis"
export {
  controlApplicationAnalysisScheduler,
  fetchApplicationAnalysisResult,
  fetchApplicationAnalysisSchedulerStatus,
  fetchApplicationAnalysisTargets,
  listApplicationAnalysisRecent30,
  listApplicationAnalysisRecent30Full,
  readApplicationAnalysisRecent30,
  refreshApplicationAnalysisRecent30,
  runApplicationAnalysis,
  saveApplicationAnalysisTargets,
  triggerApplicationAnalysis,
} from "@/services/ai/application-analysis"

export type {
  SelfSelectedGroup,
  SelfSelectedGroupActionResponse,
  SelfSelectedGroupListResponse,
  SelfSelectedItem,
  SelfSelectedItemActionResponse,
  SelfSelectedItemListResponse,
} from "@/services/market/self-selected"
export {
  createSelfSelectedGroup,
  createSelfSelectedItem,
  deleteSelfSelectedGroup,
  deleteSelfSelectedItem,
  fetchSelfSelectedGroups,
  fetchSelfSelectedItems,
  updateSelfSelectedGroup,
  updateSelfSelectedItem,
} from "@/services/market/self-selected"

export type {
  SchedulerCategory,
  SchedulerDailyStatItem,
  SchedulerJobHistoryItem,
  SchedulerJobItem,
} from "@/services/scheduler/management"
export {
  deleteSchedulerJob,
  disableSchedulerJob,
  enableSchedulerJob,
  fetchSchedulerCategories,
  fetchSchedulerDailyStats,
  fetchSchedulerJobHistory,
  fetchSchedulerJobs,
  startSchedulerJob,
  stopSchedulerJob,
  triggerSchedulerJob,
} from "@/services/scheduler/management"

export type {
  AuctionAiAnalysisResponse,
  MarketBreadthResponse,
  SectorMeta,
  SectorStock,
  SectorStocksResponse,
  StockFundFlowPoint,
  StockFundFlowResponse,
  StockMetaResponse,
  StockSectorEntry,
  StockSectorsResponse,
} from "@/services/market/stock-chart"
export {
  createStockAnnotation,
  deleteStockAnnotation,
  fetchAuctionAiAnalysisSnapshot,
  fetchMarketBreadth,
  fetchMarketBreadthSeries,
  fetchSectorStocks,
  fetchStockAuction,
  fetchStockFundFlow,
  fetchStockIntraday,
  fetchStockKlines,
  fetchStockMeta,
  fetchStockSectors,
  listStockAnnotations,
  runAuctionAiAnalysis,
  searchStockChart,
  triggerAuctionAiAnalysisScheduler,
} from "@/services/market/stock-chart"

export type {
  IndexDailyItem,
  IndexDailyResponse,
  IndexKlineBatchResponse,
  IndexKlineItem,
  IndexKlinePoint,
  IndexReturnsHistoryItem,
  IndexReturnsHistoryResponse,
  MaCountBoardStat,
  MaCountHistoryItem,
  MaCountHistoryResponse,
  MaCountResponse,
  MarketOverviewHistoryItem,
  MarketOverviewHistoryResponse,
  MarketSentimentIndexHistoryItem,
  MarketSentimentIndexHistoryResponse,
  MarketSentimentIndexResponse,
  ProfitEffectHistoryItem,
  ProfitEffectHistoryResponse,
  ProfitEffectResponse,
  RiskAppetiteHistoryItem,
  RiskAppetiteHistoryResponse,
  RiskAppetiteResponse,
  SectorBreadthHistoryResponse,
  SectorBreadthItem,
  SectorBreadthResponse,
  StyleRiskAppetiteHistoryItem,
  StyleRiskAppetiteHistoryResponse,
} from "@/services/market/sentiment"
export {
  fetchIndexDailyHistory,
  fetchMarketOverviewHistory,
  fetchMarketPulseIndexReturns,
  fetchMarketPulseIndexReturnsHistory,
  fetchMarketSentimentIndex,
  fetchMarketSentimentIndexHistory,
  fetchMarketSentimentLimitEmotionSummary,
  fetchMarketSentimentLimitEmotionSummaryHistory,
  fetchMarketSentimentMaCount,
  fetchMarketSentimentMaCountHistory,
  fetchMarketSentimentProfitEffect,
  fetchMarketSentimentProfitEffectHistory,
  fetchMarketSentimentRiskAppetite,
  fetchMarketSentimentRiskAppetiteHistory,
  fetchMarketSentimentSectorBreadth,
  fetchMarketSentimentSectorBreadthHistory,
  fetchMarketSentimentStyleRiskAppetite,
  fetchMarketSentimentStyleRiskAppetiteHistory,
  fetchMarketSentimentTurnoverActivity,
  fetchMarketSentimentTurnoverActivityHistory,
  fetchMarketSentimentVolatilitySentiment,
  fetchMarketSentimentVolatilitySentimentHistory,
  fetchIndexKlineBatch,
  triggerMarketOverviewAkshareRefresh,
} from "@/services/market/sentiment"

export type {
  HeatmapKind,
  IndustryCompareResponse,
  IndustryConstituentRow,
  IndustryConstituentsIndexResponse,
  IndustryDetail,
  IndexReturnItem,
  IndexReturnsResponse,
  LimitEmotionPayload,
  ManualFundFlow,
  MarketHeatmapItem,
  MarketHeatmapResponse,
  MarketHistoryPoint,
  MarketHistoryResponse,
  MarketOverview,
  MarketOverviewEltdx,
  MarketPulse,
  MarketPulseOptions,
  RotationRow,
  RotationTrendData,
  SchedulerStatus,
  StyleSectorConstituent,
  StyleSectorConstituentsResponse,
  StyleSectorItem,
  StyleSectorListResponse,
  StrongRow,
  FlowRow,
} from "@/services/market/pulse"
export {
  fetchIndustryConstituentsByCode,
  fetchIndustryDetail,
  fetchIndustryFundFlowIndustryList,
  fetchIndexKlineBatch as fetchMarketPulseIndexKlineBatch,
  fetchManualFundFlow,
  fetchMarketHeatmap,
  fetchMarketOverviewAkshare,
  fetchMarketOverviewAkshareArchive,
  fetchMarketOverviewEltdx,
  fetchMarketPulse,
  fetchMarketPulseHistory,
  fetchMarketPulseIndustryCompare,
  fetchMarketPulseLimitEmotion,
  fetchMarketPulseRotationTrend,
  fetchMarketPulseSchedulerStatus,
  fetchStyleSectorConstituents,
  fetchStyleSectors,
  refreshMarketPulseLimitEmotion,
  saveManualFundFlow,
  triggerMarketPulseSnapshot,
} from "@/services/market/pulse"

export type {
  IndustryApplicationConfig,
  IndustryApplicationIndicators,
  IndustryApplicationIndexBar,
  IndustryApplicationKlinePayload,
  MarketHeatmapResponse as IndustryApplicationHeatmapResponse,
  IndustryApplicationOverviewResponse,
  IndustryApplicationTargetCode,
} from "@/views/industry-application/lib/types"
export type {
  IndustryApplicationRefreshResponse,
  IndustryApplicationResultResponse,
  IndustryFundFlowHistoryResponse,
  IndustryFundFlowResponse,
  IndustryFundFlowRow,
} from "@/services/market/industry-application"
export {
  fetchIndustryApplicationKline,
  fetchIndustryApplicationOverview,
  fetchIndustryApplicationResult,
  fetchIndustryApplicationTargetCodes,
  fetchIndustryApplicationTargets,
  fetchIndustryFundFlow,
  fetchIndustryFundFlowHistory,
  refreshIndustryApplication,
  refreshIndustryFundFlow,
  saveIndustryApplicationTargets,
  fetchMarketHeatmap as fetchIndustryApplicationHeatmap,
} from "@/services/market/industry-application"

export type { DownloaderParseData, RemoteParsePayload } from "@/services/legacy/downloader"
export { parseDownloaderUrl } from "@/services/legacy/downloader"

export type { TaskSnapshot } from "@/services/legacy/mp4-workspace"
export {
  askQuestion,
  checkStatus,
  createSSEConnection,
  exportMarkdown,
  fetchTaskSnapshot,
  saveMP4History,
  sendDownloaderResultToParse,
  uploadFile,
  uploadFileWithProgress,
} from "@/services/legacy/mp4-workspace"

export {
  askHistoryQuestion,
  deleteMP4History,
  getMP4History,
  listMP4History,
  reorderMP4History,
} from "@/services/legacy/mp4-history"

export { fetchMarketOverview } from "@/services/legacy/market-overview"

export type {
  StockAnnouncementsResponse,
  StockBusinessCompositionItem,
  StockBusinessCompositionResponse,
  StockCompanyNewsResponse,
  StockFinanceReportResponse,
  StockNewsResponse,
  StockProfitForecastResponse,
  StockRoadshowsResponse,
  StockValuationResponse,
} from "@/services/market/f10"
export {
  fetchStockAnnouncements,
  fetchStockBusinessComposition,
  fetchStockCompanyNews,
  fetchStockFinanceReport,
  fetchStockNews,
  fetchStockProfitForecast,
  fetchStockRoadshows,
  fetchStockValuation,
} from "@/services/market/f10"
