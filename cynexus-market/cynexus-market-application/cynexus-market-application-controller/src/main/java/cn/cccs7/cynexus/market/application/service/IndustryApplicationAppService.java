package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.IndustryApplicationDTO;
import cn.cccs7.cynexus.market.api.request.IndustryApplicationRequest;
import cn.cccs7.cynexus.market.application.convert.IndustryApplicationDTOConverter;
import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import cn.cccs7.cynexus.market.domain.repository.IndustryApplicationRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndustryApplicationAppService {

    private static final int REFRESH_KLINE_COUNT = 240;

    private final IndustryApplicationRepository industryApplicationRepository;
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
        List<IndustryApplicationBO.Target> targets = resolveRefreshTargets(targetId);
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
                .fetchedAt(OffsetDateTime.now())
                .source("clickhouse.daily_raw")
                .build();
    }

    public IndustryApplicationDTO.FundFlowResponse getFundFlow(String tradeDate, Integer top) {
        LocalDate parsedTradeDate = parseTradeDate(tradeDate);
        Integer normalizedTop = top == null || top <= 0 ? null : Math.min(top, 500);
        return converter.toFundFlowResponse(industryApplicationRepository.getFundFlow(parsedTradeDate, normalizedTop));
    }

    public IndustryApplicationDTO.FundFlowHistoryResponse getFundFlowHistory(String maxTradeDate, int days, Integer top) {
        LocalDate parsedMaxTradeDate = parseTradeDate(maxTradeDate);
        int normalizedDays = Math.max(1, Math.min(days <= 0 ? 365 : days, 500));
        Integer normalizedTop = top == null || top <= 0 ? null : Math.min(top, 500);
        return converter.toFundFlowHistoryResponse(
                industryApplicationRepository.getFundFlowHistory(parsedMaxTradeDate, normalizedDays, normalizedTop)
        );
    }

    public IndustryApplicationDTO.TdxFundFlowResponse getTdxFundFlow(String targetType, String symbol, String tradeDate, int days) {
        LocalDate parsedTradeDate = parseTradeDate(tradeDate);
        int normalizedDays = Math.max(1, Math.min(days <= 0 ? 30 : days, 252));
        return converter.toTdxFundFlowResponse(
                industryApplicationRepository.getTdxFundFlow(targetType, symbol, parsedTradeDate, normalizedDays)
        );
    }

    private List<IndustryApplicationBO.Target> resolveRefreshTargets(String targetId) {
        if (targetId != null && !targetId.isBlank()) {
            return industryApplicationRepository.findTarget(targetId.trim()).stream().toList();
        }
        return industryApplicationRepository.getConfig().getItems().stream()
                .filter(item -> item.getEnabled() == null || item.getEnabled())
                .toList();
    }

    private LocalDate parseTradeDate(String tradeDate) {
        if (tradeDate == null || tradeDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(tradeDate.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("trade_date must be yyyy-MM-dd");
        }
    }

    private IndustryApplicationDTO.RefreshItem refreshOne(IndustryApplicationBO.Target target) {
        if (target == null) {
            return IndustryApplicationDTO.RefreshItem.builder()
                    .ok(false)
                    .error("target not found")
                    .build();
        }
        try {
            IndustryApplicationBO.Kline kline = industryApplicationRepository.getKline(
                    target.getTargetType(),
                    target.getSymbol(),
                    "day",
                    REFRESH_KLINE_COUNT
            );
            industryApplicationRepository.saveResult(target, kline);
            return IndustryApplicationDTO.RefreshItem.builder()
                    .id(target.getTargetKey())
                    .ok(true)
                    .klineCount(kline.getBars() == null ? 0 : kline.getBars().size())
                    .build();
        } catch (Exception ex) {
            return IndustryApplicationDTO.RefreshItem.builder()
                    .id(target.getTargetKey())
                    .ok(false)
                    .klineCount(0)
                    .error(ex.getMessage())
                    .build();
        }
    }
}
