package cn.cccs7.cynexus.market.domain.service.industry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import cn.cccs7.cynexus.market.domain.repository.IndustryApplicationRepository;
import cn.cccs7.cynexus.market.domain.service.industry.impl.IndustryApplicationQueryDomainServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IndustryApplicationQueryDomainServiceTest {

    @Test
    void normalizesFundFlowArgumentsBeforeRepositoryCall() {
        LocalDate[] date = new LocalDate[1];
        Integer[] top = new Integer[1];
        IndustryApplicationRepository repository = new FakeRepository() {
            @Override public IndustryApplicationBO.FundFlowSnapshot getFundFlow(LocalDate requestedDate, Integer requestedTop) {
                date[0] = requestedDate;
                top[0] = requestedTop;
                return null;
            }
        };

        new IndustryApplicationQueryDomainServiceImpl(repository).getFundFlow(" 2026-07-23 ", 9999);

        assertEquals(LocalDate.parse("2026-07-23"), date[0]);
        assertEquals(500, top[0]);
    }

    @Test
    void clampsTdxHistoryDays() {
        int[] days = new int[1];
        IndustryApplicationRepository repository = new FakeRepository() {
            @Override public IndustryApplicationBO.TdxFundFlowSnapshot getTdxFundFlow(String targetType, String symbol,
                                                                                        LocalDate tradeDate, int requestedDays) {
                days[0] = requestedDays;
                return null;
            }
        };

        new IndustryApplicationQueryDomainServiceImpl(repository).getTdxFundFlow("stock", "600519", null, 9999);

        assertEquals(252, days[0]);
    }

    private static class FakeRepository implements IndustryApplicationRepository {
        @Override public IndustryApplicationBO.Config getConfig() { return null; }
        @Override public IndustryApplicationBO.Config saveConfig(IndustryApplicationBO.Config config) { return config; }
        @Override public List<IndustryApplicationBO.TargetCode> listTargetCodes(String kind) { return List.of(); }
        @Override public Optional<IndustryApplicationBO.Target> findTarget(String targetKey) { return Optional.empty(); }
        @Override public IndustryApplicationBO.Kline getKline(String targetType, String symbol, String period, int count) { return null; }
        @Override public Optional<IndustryApplicationBO.Result> findResult(String targetKey) { return Optional.empty(); }
        @Override public void saveResult(IndustryApplicationBO.Target target, IndustryApplicationBO.Kline kline) { }
        @Override public List<IndustryApplicationBO.SectorOverviewItem> listOverview(String sortBy, boolean ascending, int count) { return List.of(); }
        @Override public IndustryApplicationBO.FundFlowSnapshot getFundFlow(LocalDate tradeDate, Integer top) { return null; }
        @Override public IndustryApplicationBO.FundFlowHistory getFundFlowHistory(LocalDate maxTradeDate, int days, Integer top) { return null; }
        @Override public IndustryApplicationBO.TdxFundFlowSnapshot getTdxFundFlow(String targetType, String symbol, LocalDate tradeDate, int days) { return null; }
    }
}
