package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketInformationBO;

public interface MarketInformationRepository {
    MarketInformationBO.Feed feed(int page, int pageSize, String impactIndustryCode, String analysisStatus);
    MarketInformationBO.Brief latestBrief();
    MarketInformationBO.EventDetail eventDetail(long clusterId);
    java.util.List<MarketInformationBO.News> analysisStatuses(java.util.List<Long> clusterIds);
    boolean requestAnalysis(long clusterId, boolean force, String requestedBy);
    int refreshPipeline();
}
