package cn.cccs7.cynexus.market.domain.service.industry;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import java.util.List;
import java.util.Optional;

public interface IndustryApplicationQueryDomainService {

    IndustryApplicationBO.Config getTargets();

    IndustryApplicationBO.Config saveTargets(IndustryApplicationBO.Config config);

    List<IndustryApplicationBO.TargetCode> listTargetCodes(String kind);

    IndustryApplicationBO.Kline getKline(String targetType, String symbol, String period, int count);

    Optional<IndustryApplicationBO.Result> getResult(String targetId);

    List<IndustryApplicationBO.SectorOverviewItem> getOverview(String sortBy, boolean ascending, int count);

    IndustryApplicationBO.FundFlowSnapshot getFundFlow(String tradeDate, Integer top);

    IndustryApplicationBO.FundFlowHistory getFundFlowHistory(String maxTradeDate, int days, Integer top);

    IndustryApplicationBO.TdxFundFlowSnapshot getTdxFundFlow(String targetType, String symbol, String tradeDate, int days);
}
