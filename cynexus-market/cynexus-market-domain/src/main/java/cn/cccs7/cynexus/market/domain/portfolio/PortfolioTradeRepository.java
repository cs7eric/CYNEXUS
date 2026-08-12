package cn.cccs7.cynexus.market.domain.portfolio;

public interface PortfolioTradeRepository {
    boolean save(String principalKey, String tenantKey, long sourceTaskId, long sourceCandidateId,
                 PortfolioTradeBO trade, String confirmedBy);

    default boolean update(String principalKey, String tenantKey, PortfolioTradeBO trade) {
        return false;
    }
}
