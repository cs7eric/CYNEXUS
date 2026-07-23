package cn.cccs7.cynexus.market.domain.service.industry.impl;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import cn.cccs7.cynexus.market.domain.industry.IndustryApplicationDomainPolicy;
import cn.cccs7.cynexus.market.domain.repository.IndustryApplicationRepository;
import cn.cccs7.cynexus.market.domain.service.industry.IndustryApplicationRefreshDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IndustryApplicationRefreshDomainServiceImpl implements IndustryApplicationRefreshDomainService {

    private final IndustryApplicationRepository repository;
    private final IndustryApplicationDomainPolicy policy = new IndustryApplicationDomainPolicy();

    public IndustryApplicationRefreshDomainServiceImpl(IndustryApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<IndustryApplicationBO.RefreshResult> refresh(String targetId) {
        List<IndustryApplicationBO.Target> targets = policy.resolveRefreshTargets(targetId,
                repository.getConfig(), targetId == null || targetId.isBlank()
                        ? List.of() : repository.findTarget(targetId.trim()).stream().toList());
        return targets.stream().map(this::refreshOne).toList();
    }

    private IndustryApplicationBO.RefreshResult refreshOne(IndustryApplicationBO.Target target) {
        IndustryApplicationBO.Kline kline = null;
        Exception failure = null;
        try {
            kline = repository.getKline(target.getTargetType(), target.getSymbol(), "day",
                    IndustryApplicationDomainPolicy.REFRESH_KLINE_COUNT);
            repository.saveResult(target, kline);
        } catch (Exception ex) {
            failure = ex;
        }
        var decision = policy.refreshDecision(target, kline, failure);
        return new IndustryApplicationBO.RefreshResult(decision.id(), decision.ok(), decision.klineCount(), decision.error());
    }
}
