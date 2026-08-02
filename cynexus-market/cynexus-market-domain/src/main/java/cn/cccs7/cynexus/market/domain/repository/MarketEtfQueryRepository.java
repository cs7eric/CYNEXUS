package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketEtfDataBO;
import java.util.Optional;

public interface MarketEtfQueryRepository {

    MarketEtfDataBO load(int historyDays, int rowLimit);

    default Optional<String> findName(String code) {
        return Optional.empty();
    }
}
