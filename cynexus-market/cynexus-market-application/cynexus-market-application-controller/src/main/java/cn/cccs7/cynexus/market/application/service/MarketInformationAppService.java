package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.MarketInformationDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketInformationBO;
import cn.cccs7.cynexus.market.domain.service.information.MarketInformationQueryDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketInformationAppService {
    private final MarketInformationQueryDomainService domainService;
    public MarketInformationAppService(MarketInformationQueryDomainService domainService) { this.domainService = domainService; }
    public MarketInformationDTO.FeedDTO feed(Integer page, Integer pageSize, String impactIndustryCode, String analysisStatus) {
        MarketInformationBO.Feed value = domainService.feed(page, pageSize, impactIndustryCode, analysisStatus);
        List<MarketInformationDTO.NewsDTO> news = mapNews(value.getNews());
        MarketInformationBO.NewsPage newsPage = value.getNewsPage();
        return MarketInformationDTO.FeedDTO.builder().queriedAt(value.getQueriedAt())
            .brief(mapBrief(value.getBrief()))
            .newsPage(MarketInformationDTO.NewsPageDTO.builder().items(news).total(newsPage.getTotal()).page(newsPage.getPage())
                .pageSize(newsPage.getPageSize()).totalPages(newsPage.getTotalPages()).hasPrevious(newsPage.isHasPrevious()).hasNext(newsPage.isHasNext()).build())
            .news(news)
            .macroNews(news)
            .policyAnnouncements(value.getPolicyAnnouncements() == null ? List.of() : value.getPolicyAnnouncements().stream().map(item -> MarketInformationDTO.PolicyAnnouncementDTO.builder().id(item.getId()).title(item.getTitle()).category(item.getCategory()).publishedDate(item.getPublishedDate()).url(item.getUrl()).issuer(item.getIssuer()).build()).toList())
            .signals(value.getSignals() == null ? List.of() : value.getSignals().stream().map(item -> MarketInformationDTO.SignalDTO.builder().code(item.getCode()).name(item.getName()).pool(item.getPool()).reason(item.getReason()).changePct(item.getChangePct()).sealAmount(item.getSealAmount()).snapshotDate(item.getSnapshotDate()).build()).toList())
            .hotStocks(value.getHotStocks() == null ? List.of() : value.getHotStocks().stream().map(item -> MarketInformationDTO.HotStockDTO.builder().code(item.getCode()).name(item.getName()).rank(item.getRank()).heat(item.getHeat()).pct(item.getPct()).snapshotDate(item.getSnapshotDate()).build()).toList())
            .leaders(value.getLeaders() == null ? List.of() : value.getLeaders().stream().map(item -> MarketInformationDTO.HotStockDTO.builder().code(item.getCode()).name(item.getName()).rank(item.getRank()).heat(item.getHeat()).pct(item.getPct()).snapshotDate(item.getSnapshotDate()).build()).toList())
            .leaderEvents(value.getLeaderEvents() == null ? List.of() : value.getLeaderEvents().stream().map(item -> MarketInformationDTO.LeaderEventDTO.builder().code(item.getCode()).name(item.getName()).title(item.getTitle()).summary(item.getSummary()).source(item.getSource()).publishedAt(item.getPublishedAt()).url(item.getUrl()).build()).toList())
            .dragonTiger(value.getDragonTiger() == null ? List.of() : value.getDragonTiger().stream().map(item -> MarketInformationDTO.DragonTigerDTO.builder().code(item.getCode()).name(item.getName()).reason(item.getReason()).changePct(item.getChangePct()).netBuyWan(item.getNetBuyWan()).tradeDate(item.getTradeDate()).build()).toList())
            .impactIndustries(mapImpacts(value.getImpactIndustries())).build();
    }

    private List<MarketInformationDTO.NewsDTO> mapNews(List<MarketInformationBO.News> items) {
        return items == null ? List.of() : items.stream().map(item -> MarketInformationDTO.NewsDTO.builder()
                .id(item.getId()).title(item.getTitle()).summary(item.getSummary()).source(item.getSource())
                .category(item.getCategory()).kind(item.getKind()).issuer(item.getIssuer()).labels(item.getLabels())
                .publishedAt(item.getPublishedAt()).url(item.getUrl()).clusterId(item.getClusterId())
                .analysisStatus(item.getAnalysisStatus()).materiality(item.getMateriality())
                .marketDirection(item.getMarketDirection()).impactStatus(item.getImpactStatus())
                .impactIndustries(mapImpacts(item.getImpactIndustries())).build()).toList();
    }

    public MarketInformationDTO.BriefDTO latestBrief() {
        return mapBrief(domainService.latestBrief());
    }

    public MarketInformationDTO.EventDetailDTO eventDetail(long clusterId) {
        MarketInformationBO.EventDetail value = domainService.eventDetail(clusterId);
        if (value == null) return null;
        return MarketInformationDTO.EventDetailDTO.builder()
                .clusterId(value.getClusterId()).title(value.getTitle()).category(value.getCategory())
                .entities(value.getEntities()).priorityScore(value.getPriorityScore()).highValue(value.getHighValue())
                .firstPublishedAt(value.getFirstPublishedAt()).lastPublishedAt(value.getLastPublishedAt())
                .contentHash(value.getContentHash()).analysisStatus(value.getAnalysisStatus())
                .providerCode(value.getProviderCode()).modelCode(value.getModelCode()).fallbackUsed(value.getFallbackUsed())
                .analysisJson(value.getAnalysisJson())
                .impactIndustries(mapImpacts(value.getImpactIndustries()))
                .members(value.getMembers() == null ? List.of() : value.getMembers().stream().map(item ->
                        MarketInformationDTO.EventMemberDTO.builder().source(item.getSource()).sourceRecordId(item.getSourceRecordId())
                                .title(item.getTitle()).summary(item.getSummary()).url(item.getUrl()).publishedAt(item.getPublishedAt())
                                .contentStatus(item.getContentStatus()).contentQuality(item.getContentQuality()).build()).toList())
                .build();
    }

    private List<MarketInformationDTO.ImpactIndustryDTO> mapImpacts(List<MarketInformationBO.ImpactIndustry> items) {
        return items == null ? List.of() : items.stream().map(item -> MarketInformationDTO.ImpactIndustryDTO.builder()
                .tdxCode(item.getTdxCode()).tdxName(item.getTdxName()).thsCode(item.getThsCode()).thsName(item.getThsName())
                .status(item.getStatus()).source(item.getSource()).confidence(item.getConfidence())
                .relatedStocks(item.getRelatedStocks() == null ? List.of() : item.getRelatedStocks().stream().map(stock -> MarketInformationDTO.RelatedStockDTO.builder().code(stock.getCode()).name(stock.getName()).build()).toList()).build()).toList();
    }

    public boolean requestAnalysis(long clusterId, boolean force) {
        return domainService.requestAnalysis(clusterId, force, "market-ui");
    }

    public List<MarketInformationDTO.AnalysisStatusDTO> analysisStatuses(List<Long> clusterIds) {
        if (clusterIds == null || clusterIds.isEmpty()) return List.of();
        return domainService.analysisStatuses(clusterIds).stream().map(item -> MarketInformationDTO.AnalysisStatusDTO.builder()
                .clusterId(item.getClusterId()).analysisStatus(item.getAnalysisStatus())
                .materiality(item.getMateriality()).marketDirection(item.getMarketDirection()).build()).toList();
    }

    public int refreshPipeline() {
        return domainService.refreshPipeline();
    }

    private MarketInformationDTO.BriefDTO mapBrief(MarketInformationBO.Brief value) {
        if (value == null) return null;
        return MarketInformationDTO.BriefDTO.builder().briefDate(value.getBriefDate()).status(value.getStatus())
                .providerCode(value.getProviderCode()).modelCode(value.getModelCode()).fallbackUsed(value.getFallbackUsed())
                .contentJson(value.getContentJson()).updatedAt(value.getUpdatedAt()).build();
    }
}
