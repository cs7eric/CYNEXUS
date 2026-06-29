package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IndustryApplicationRepository {

    IndustryApplicationBO.Config getConfig();

    IndustryApplicationBO.Config saveConfig(IndustryApplicationBO.Config config);

    List<IndustryApplicationBO.TargetCode> listTargetCodes(String kind);

    Optional<IndustryApplicationBO.Target> findTarget(String targetKey);

    IndustryApplicationBO.Kline getKline(String targetType, String symbol, String period, int count);

    Optional<IndustryApplicationBO.Result> findResult(String targetKey);

    void saveResult(IndustryApplicationBO.Target target, IndustryApplicationBO.Kline kline);

    List<IndustryApplicationBO.SectorOverviewItem> listOverview(String sortBy, boolean ascending, int count);

    IndustryApplicationBO.FundFlowSnapshot getFundFlow(LocalDate tradeDate, Integer top);
}
