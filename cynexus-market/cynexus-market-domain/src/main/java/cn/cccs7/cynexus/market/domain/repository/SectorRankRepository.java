package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import java.util.List;

public interface SectorRankRepository {

    List<SectorRankBO> findByTradeDate(String tradeDate, int limit);
}
