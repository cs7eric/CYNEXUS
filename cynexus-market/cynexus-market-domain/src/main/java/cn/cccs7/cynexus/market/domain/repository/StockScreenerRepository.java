package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockScreenerRepository {

    Optional<StockScreenerQueryBO.Snapshot> findSnapshotOnOrBefore(LocalDate requestedTradeDate,
                                                                     StockScreenerQueryBO.PriceVolumePattern priceVolumePattern,
                                                                     int dailyHistorySessions);

    StockScreenerQueryBO.ClassificationOptions findClassificationOptionsOnOrBefore(LocalDate requestedTradeDate);
}
