package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.StockBasicPO;
import java.util.List;

public interface StockBasicBasicService extends CrudBasicService<StockBasicPO> {

    List<StockBasicPO> searchForStockChart(String keyword, int limit);

    StockBasicPO findByCodeForStockChart(String code);
}
