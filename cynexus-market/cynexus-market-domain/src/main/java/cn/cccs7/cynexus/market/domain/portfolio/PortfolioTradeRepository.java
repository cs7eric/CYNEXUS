package cn.cccs7.cynexus.market.domain.portfolio;

public interface PortfolioTradeRepository {
    void save(String principalKey, String tenantKey, long sourceTaskId, long sourceCandidateId,
              PortfolioTradeBO trade, String confirmedBy);
}
