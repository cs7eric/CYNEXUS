package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.List;

public interface PortfolioCashFlowRepository {
    List<PortfolioCashFlowBO> list(String principalKey, String tenantKey);
    PortfolioCashFlowBO save(String principalKey, String tenantKey, PortfolioCashFlowBO flow);
}
