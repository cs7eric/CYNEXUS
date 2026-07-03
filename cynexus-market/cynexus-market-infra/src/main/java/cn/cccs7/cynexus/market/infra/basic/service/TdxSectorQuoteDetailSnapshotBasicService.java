package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxSectorQuoteDetailSnapshotPO;
import java.util.Optional;

public interface TdxSectorQuoteDetailSnapshotBasicService extends CrudBasicService<TdxSectorQuoteDetailSnapshotPO> {

    Optional<TdxSectorQuoteDetailSnapshotPO> findLatestByCode6(String code6);
}
