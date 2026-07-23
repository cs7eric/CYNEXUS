package cn.cccs7.cynexus.market.domain.service.information.impl;

import cn.cccs7.cynexus.market.domain.entity.MarketInformationBO;
import cn.cccs7.cynexus.market.domain.repository.MarketInformationRepository;
import cn.cccs7.cynexus.market.domain.service.information.MarketInformationQueryDomainService;
import java.util.List;

public class MarketInformationQueryDomainServiceImpl implements MarketInformationQueryDomainService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 60;

    private final MarketInformationRepository repository;

    public MarketInformationQueryDomainServiceImpl(MarketInformationRepository repository) {
        this.repository = repository;
    }

    @Override
    public MarketInformationBO.Feed feed(Integer page, Integer pageSize, String impactIndustryCode, String analysisStatus) {
        int safePage = page == null || page <= 0 ? DEFAULT_PAGE : page;
        int safePageSize = pageSize == null || pageSize <= 0
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        return repository.feed(safePage, safePageSize, impactIndustryCode, analysisStatus);
    }

    @Override
    public MarketInformationBO.Brief latestBrief() {
        return repository.latestBrief();
    }

    @Override
    public MarketInformationBO.EventDetail eventDetail(long clusterId) {
        return repository.eventDetail(clusterId);
    }

    @Override
    public List<MarketInformationBO.News> analysisStatuses(List<Long> clusterIds) {
        return clusterIds == null || clusterIds.isEmpty() ? List.of() : repository.analysisStatuses(clusterIds);
    }

    @Override
    public boolean requestAnalysis(long clusterId, boolean force, String requestedBy) {
        return repository.requestAnalysis(clusterId, force,
                requestedBy == null || requestedBy.isBlank() ? "market-ui" : requestedBy.trim());
    }

    @Override
    public int refreshPipeline() {
        return repository.refreshPipeline();
    }
}
