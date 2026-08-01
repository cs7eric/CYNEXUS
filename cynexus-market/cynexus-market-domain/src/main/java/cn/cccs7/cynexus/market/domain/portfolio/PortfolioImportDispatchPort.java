package cn.cccs7.cynexus.market.domain.portfolio;

public interface PortfolioImportDispatchPort {
    void dispatch(long taskId, String principalKey, String tenantKey);
}
