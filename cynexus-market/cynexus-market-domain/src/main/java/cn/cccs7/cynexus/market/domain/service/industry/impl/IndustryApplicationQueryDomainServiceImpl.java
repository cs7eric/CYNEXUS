package cn.cccs7.cynexus.market.domain.service.industry.impl;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import cn.cccs7.cynexus.market.domain.industry.IndustryApplicationDomainPolicy;
import cn.cccs7.cynexus.market.domain.repository.IndustryApplicationRepository;
import cn.cccs7.cynexus.market.domain.service.industry.IndustryApplicationQueryDomainService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class IndustryApplicationQueryDomainServiceImpl implements IndustryApplicationQueryDomainService {

    private final IndustryApplicationRepository repository;
    private final IndustryApplicationDomainPolicy policy = new IndustryApplicationDomainPolicy();

    public IndustryApplicationQueryDomainServiceImpl(IndustryApplicationRepository repository) {
        this.repository = repository;
    }

    @Override public IndustryApplicationBO.Config getTargets() { return repository.getConfig(); }
    @Override public IndustryApplicationBO.Config saveTargets(IndustryApplicationBO.Config config) { return repository.saveConfig(config); }
    @Override public List<IndustryApplicationBO.TargetCode> listTargetCodes(String kind) { return repository.listTargetCodes(kind); }
    @Override public IndustryApplicationBO.Kline getKline(String targetType, String symbol, String period, int count) {
        return repository.getKline(targetType, symbol, period, count);
    }
    @Override public Optional<IndustryApplicationBO.Result> getResult(String targetId) { return repository.findResult(targetId); }
    @Override public List<IndustryApplicationBO.SectorOverviewItem> getOverview(String sortBy, boolean ascending, int count) {
        return repository.listOverview(sortBy, ascending, Math.max(1, Math.min(count <= 0 ? 100 : count, 500)));
    }
    @Override public IndustryApplicationBO.FundFlowSnapshot getFundFlow(String tradeDate, Integer top) {
        return repository.getFundFlow(policy.parseTradeDate(tradeDate), policy.normalizeTop(top));
    }
    @Override public IndustryApplicationBO.FundFlowHistory getFundFlowHistory(String maxTradeDate, int days, Integer top) {
        return repository.getFundFlowHistory(policy.parseTradeDate(maxTradeDate), policy.normalizeHistoryDays(days), policy.normalizeTop(top));
    }
    @Override public IndustryApplicationBO.TdxFundFlowSnapshot getTdxFundFlow(String targetType, String symbol, String tradeDate, int days) {
        return repository.getTdxFundFlow(targetType, symbol, policy.parseTradeDate(tradeDate), Math.max(1, Math.min(days <= 0 ? 30 : days, 252)));
    }
}
