package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.IndustryApplicationDTO;
import cn.cccs7.cynexus.market.api.request.IndustryApplicationRequest;
import cn.cccs7.cynexus.market.application.convert.IndustryApplicationDTOConverter;
import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import cn.cccs7.cynexus.market.domain.repository.IndustryApplicationRepository;
import cn.cccs7.cynexus.market.domain.industry.IndustryApplicationDomainPolicy;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndustryApplicationAppService {

    private final IndustryApplicationRepository industryApplicationRepository;
    private final IndustryApplicationDomainPolicy domainPolicy = new IndustryApplicationDomainPolicy();
    private final IndustryApplicationDTOConverter converter = IndustryApplicationDTOConverter.INSTANCE;

    public IndustryApplicationAppService(IndustryApplicationRepository industryApplicationRepository) {
        this.industryApplicationRepository = industryApplicationRepository;
    }

    public IndustryApplicationDTO.ConfigResponse getTargets() {
        return converter.toConfigResponse(industryApplicationRepository.getConfig());
    }

    @Transactional
    public IndustryApplicationDTO.ConfigResponse saveTargets(IndustryApplicationRequest.SaveTargetsRequest request) {
        IndustryApplicationBO.Config value = converter.toConfigBO(
                request == null ? null : request.getHorizon(),
                request == null ? List.of() : request.getItems()
        );
        return converter.toConfigResponse(industryApplicationRepository.saveConfig(value));
    }

    public IndustryApplicationDTO.TargetCodesResponse listTargetCodes(String kind) {
        List<IndustryApplicationDTO.TargetCode> items = converter.toTargetCodes(industryApplicationRepository.listTargetCodes(kind));
        return IndustryApplicationDTO.TargetCodesResponse.builder()
                .items(items)
                .count(items.size())
                .source("clickhouse.daily_raw+postgres.target_catalog")
                .build();
    }

    public IndustryApplicationDTO.KlineResponse getKline(String targetType, String symbol, String period, int count) {
        return converter.toKlineResponse(industryApplicationRepository.getKline(targetType, symbol, period, count));
    }

    @Transactional
    public IndustryApplicationDTO.RefreshResponse refresh(IndustryApplicationRequest.RefreshRequest request) {
        String targetId = request == null ? null : request.getTargetId();
        List<IndustryApplicationBO.Target> targets = domainPolicy.resolveRefreshTargets(
                targetId,
                industryApplicationRepository.getConfig(),
                targetId == null || targetId.isBlank() ? List.of() : industryApplicationRepository.findTarget(targetId.trim()).stream().toList()
        );
        List<IndustryApplicationDTO.RefreshItem> items = targets.stream()
                .map(this::refreshOne)
                .toList();
        boolean ok = items.stream().allMatch(item -> Boolean.TRUE.equals(item.getOk()));
        return IndustryApplicationDTO.RefreshResponse.builder()
                .ok(ok)
                .items(items)
                .count(items.size())
                .error(ok ? null : "some targets failed")
                .build();
    }

    public Optional<IndustryApplicationDTO.ResultResponse> getResult(String targetId) {
        return industryApplicationRepository.findResult(targetId).map(converter::toResultResponse);
    }

    public IndustryApplicationDTO.OverviewResponse getOverview(String sortBy, boolean ascending, int count) {
        List<IndustryApplicationBO.SectorOverviewItem> rows = industryApplicationRepository.listOverview(sortBy, ascending, count);
        List<IndustryApplicationDTO.SectorOverviewItem> items = converter.toOverviewItems(rows);
        return IndustryApplicationDTO.OverviewResponse.builder()
                .ok(true)
                .items(items)
                .industryCount((int) items.stream().filter(item -> "industry".equals(item.getKind())).count())
                .conceptCount((int) items.stream().filter(item -> "concept".equals(item.getKind())).count())
                .fetchedAt(java.time.OffsetDateTime.now())
                .source("clickhouse.daily_raw")
                .build();
    }

    public IndustryApplicationDTO.FundFlowResponse getFundFlow(String tradeDate, Integer top) {
        LocalDate parsedTradeDate = domainPolicy.parseTradeDate(tradeDate);
        Integer normalizedTop = domainPolicy.normalizeTop(top);
        return converter.toFundFlowResponse(industryApplicationRepository.getFundFlow(parsedTradeDate, normalizedTop));
    }

    public IndustryApplicationDTO.FundFlowHistoryResponse getFundFlowHistory(String maxTradeDate, int days, Integer top) {
        LocalDate parsedMaxTradeDate = domainPolicy.parseTradeDate(maxTradeDate);
        int normalizedDays = domainPolicy.normalizeHistoryDays(days);
        Integer normalizedTop = domainPolicy.normalizeTop(top);
        return converter.toFundFlowHistoryResponse(
                industryApplicationRepository.getFundFlowHistory(parsedMaxTradeDate, normalizedDays, normalizedTop)
        );
    }

    public IndustryApplicationDTO.TdxFundFlowResponse getTdxFundFlow(String targetType, String symbol, String tradeDate, int days) {
        LocalDate parsedTradeDate = domainPolicy.parseTradeDate(tradeDate);
        int normalizedDays = Math.max(1, Math.min(days <= 0 ? 30 : days, 252));
        return converter.toTdxFundFlowResponse(
                industryApplicationRepository.getTdxFundFlow(targetType, symbol, parsedTradeDate, normalizedDays)
        );
    }

    private IndustryApplicationDTO.RefreshItem refreshOne(IndustryApplicationBO.Target target) {
        IndustryApplicationBO.Kline kline = null;
        Exception failure = null;
        try {
            kline = industryApplicationRepository.getKline(
                    target.getTargetType(),
                    target.getSymbol(),
                    "day",
                    IndustryApplicationDomainPolicy.REFRESH_KLINE_COUNT
            );
            industryApplicationRepository.saveResult(target, kline);
        } catch (Exception ex) {
            failure = ex;
        }
        IndustryApplicationDomainPolicy.RefreshDecision decision = domainPolicy.refreshDecision(target, kline, failure);
        return IndustryApplicationDTO.RefreshItem.builder().id(decision.id()).ok(decision.ok())
                .klineCount(decision.klineCount()).error(decision.error()).build();
    }
}
