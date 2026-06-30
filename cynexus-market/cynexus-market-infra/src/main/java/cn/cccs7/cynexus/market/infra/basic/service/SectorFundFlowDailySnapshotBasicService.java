package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorFundFlowDailySnapshotPO;
import java.time.LocalDate;
import java.util.List;

public interface SectorFundFlowDailySnapshotBasicService extends CrudBasicService<SectorFundFlowDailySnapshotPO> {

    LocalDate findLatestTradeDate(String scope, LocalDate maxTradeDate);

    List<SectorFundFlowDailySnapshotPO> listByTradeDate(String scope, LocalDate tradeDate, int limit);

    List<SectorFundFlowDailySnapshotPO> listRecent(String scope, LocalDate maxTradeDate, int days, int top);
}
