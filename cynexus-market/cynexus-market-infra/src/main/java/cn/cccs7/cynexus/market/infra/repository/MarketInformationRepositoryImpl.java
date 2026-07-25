package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.market.domain.entity.MarketInformationBO;
import cn.cccs7.cynexus.market.domain.repository.MarketInformationRepository;
import cn.cccs7.cynexus.market.infra.client.PostgresMarketInformationClient;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Repository;

@Repository
public class MarketInformationRepositoryImpl implements MarketInformationRepository {
    private final PostgresMarketInformationClient client;
    public MarketInformationRepositoryImpl(PostgresMarketInformationClient client) { this.client = client; }
    @Override public MarketInformationBO.Feed feed(int page, int pageSize, String impactIndustryCode, String analysisStatus) {
        var macroNews = client.news(page, pageSize, impactIndustryCode, analysisStatus);
        long total = client.newsCount(impactIndustryCode, analysisStatus);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        var newsPage = MarketInformationBO.NewsPage.builder().items(macroNews).total(total).page(page).pageSize(pageSize)
                .totalPages(totalPages).hasPrevious(page > 1).hasNext(page < totalPages).build();
        int panelLimit = 30;
        var leaders = client.leaders(panelLimit);
        var dragonTiger = client.dragonTiger(panelLimit);
        return MarketInformationBO.Feed.builder().queriedAt(OffsetDateTime.now(ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID)))
                .brief(client.latestBrief())
                .newsPage(newsPage).news(macroNews).macroNews(macroNews).policyAnnouncements(client.policyAnnouncements(panelLimit)).signals(client.signals(panelLimit))
                .hotStocks(leaders).leaders(leaders).leaderEvents(client.leaderEvents(leaders, dragonTiger, panelLimit)).dragonTiger(dragonTiger)
                .impactIndustries(client.impactIndustries()).build();
    }

    @Override public MarketInformationBO.Brief latestBrief() { return client.latestBrief(); }
    @Override public MarketInformationBO.EventDetail eventDetail(long clusterId) { return client.eventDetail(clusterId); }
    @Override public java.util.List<MarketInformationBO.News> analysisStatuses(java.util.List<Long> clusterIds) { return client.analysisStatuses(clusterIds); }
    @Override public boolean requestAnalysis(long clusterId, boolean force, String requestedBy) { return client.requestAnalysis(clusterId, force, requestedBy); }
    @Override public int refreshPipeline() { return client.refreshPipeline(); }
}
