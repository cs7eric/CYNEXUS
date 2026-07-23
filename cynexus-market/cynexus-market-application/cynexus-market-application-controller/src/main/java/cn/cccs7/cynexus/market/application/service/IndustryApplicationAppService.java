package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.IndustryApplicationDTO;
import cn.cccs7.cynexus.market.api.request.IndustryApplicationRequest;
import cn.cccs7.cynexus.market.application.convert.IndustryApplicationDTOConverter;
import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import cn.cccs7.cynexus.market.domain.service.industry.IndustryApplicationQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.industry.IndustryApplicationRefreshDomainService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndustryApplicationAppService {

    private final IndustryApplicationQueryDomainService queryDomainService;
    private final IndustryApplicationRefreshDomainService refreshDomainService;
    private final IndustryApplicationDTOConverter converter = IndustryApplicationDTOConverter.INSTANCE;

    public IndustryApplicationAppService(IndustryApplicationQueryDomainService queryDomainService,
                                         IndustryApplicationRefreshDomainService refreshDomainService) {
        this.queryDomainService = queryDomainService;
        this.refreshDomainService = refreshDomainService;
    }

    public IndustryApplicationDTO.ConfigResponse getTargets() {
        return converter.toConfigResponse(queryDomainService.getTargets());
    }

    @Transactional
    public IndustryApplicationDTO.ConfigResponse saveTargets(IndustryApplicationRequest.SaveTargetsRequest request) {
        IndustryApplicationBO.Config value = converter.toConfigBO(
                request == null ? null : request.getHorizon(),
                request == null ? List.of() : request.getItems()
        );
        return converter.toConfigResponse(queryDomainService.saveTargets(value));
    }

    public IndustryApplicationDTO.TargetCodesResponse listTargetCodes(String kind) {
        List<IndustryApplicationDTO.TargetCode> items = converter.toTargetCodes(queryDomainService.listTargetCodes(kind));
        return IndustryApplicationDTO.TargetCodesResponse.builder()
                .items(items)
                .count(items.size())
                .source("clickhouse.daily_raw+postgres.target_catalog")
                .build();
    }

    public IndustryApplicationDTO.KlineResponse getKline(String targetType, String symbol, String period, int count) {
        return converter.toKlineResponse(queryDomainService.getKline(targetType, symbol, period, count));
    }

    @Transactional
    public IndustryApplicationDTO.RefreshResponse refresh(IndustryApplicationRequest.RefreshRequest request) {
        String targetId = request == null ? null : request.getTargetId();
        List<IndustryApplicationDTO.RefreshItem> items = refreshDomainService.refresh(targetId).stream()
                .map(item -> IndustryApplicationDTO.RefreshItem.builder().id(item.id()).ok(item.ok())
                        .klineCount(item.klineCount()).error(item.error()).build())
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
        return queryDomainService.getResult(targetId).map(converter::toResultResponse);
    }

    public IndustryApplicationDTO.OverviewResponse getOverview(String sortBy, boolean ascending, int count) {
        List<IndustryApplicationBO.SectorOverviewItem> rows = queryDomainService.getOverview(sortBy, ascending, count);
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
        return converter.toFundFlowResponse(queryDomainService.getFundFlow(tradeDate, top));
    }

    public IndustryApplicationDTO.FundFlowHistoryResponse getFundFlowHistory(String maxTradeDate, int days, Integer top) {
        return converter.toFundFlowHistoryResponse(queryDomainService.getFundFlowHistory(maxTradeDate, days, top));
    }

    public IndustryApplicationDTO.TdxFundFlowResponse getTdxFundFlow(String targetType, String symbol, String tradeDate, int days) {
        return converter.toTdxFundFlowResponse(queryDomainService.getTdxFundFlow(targetType, symbol, tradeDate, days));
    }
}
