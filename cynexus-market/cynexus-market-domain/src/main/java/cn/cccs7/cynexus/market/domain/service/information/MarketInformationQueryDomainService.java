package cn.cccs7.cynexus.market.domain.service.information;

import cn.cccs7.cynexus.market.domain.entity.MarketInformationBO;
import java.util.List;

public interface MarketInformationQueryDomainService {

    MarketInformationBO.Feed feed(Integer page, Integer pageSize, String impactIndustryCode, String analysisStatus);

    MarketInformationBO.Brief latestBrief();

    MarketInformationBO.EventDetail eventDetail(long clusterId);

    List<MarketInformationBO.News> analysisStatuses(List<Long> clusterIds);

    boolean requestAnalysis(long clusterId, boolean force, String requestedBy);

    int refreshPipeline();
}
