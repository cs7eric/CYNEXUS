package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.StockFundFlowSnapshotPO;
import java.util.List;

public interface StockFundFlowSnapshotBasicService extends CrudBasicService<StockFundFlowSnapshotPO> {

    StockFundFlowSnapshotPO findLatestByCode(String code);

    List<StockFundFlowSnapshotPO> listRecentByCode(String code, int limit);
}
