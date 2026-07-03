package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.market.api.dto.IndustryApplicationDTO;
import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IndustryApplicationDTOConverter {

    IndustryApplicationDTOConverter INSTANCE = Mappers.getMapper(IndustryApplicationDTOConverter.class);

    default IndustryApplicationDTO.ConfigResponse toConfigResponse(IndustryApplicationBO.Config value) {
        if (value == null) {
            return IndustryApplicationDTO.ConfigResponse.builder()
                    .version(1)
                    .horizon(IndustryApplicationDTO.Horizon.builder().days(120).segments(4).build())
                    .items(List.of())
                    .build();
        }
        return IndustryApplicationDTO.ConfigResponse.builder()
                .version(value.getVersion())
                .updatedAt(value.getUpdatedAt())
                .horizon(IndustryApplicationDTO.Horizon.builder()
                        .days(value.getHorizonDays())
                        .segments(value.getHorizonSegments())
                        .build())
                .items(toTargets(value.getItems()))
                .build();
    }

    default IndustryApplicationDTO.Target toTarget(IndustryApplicationBO.Target value) {
        if (value == null) {
            return null;
        }
        return IndustryApplicationDTO.Target.builder()
                .id(value.getTargetKey())
                .targetType(value.getTargetType())
                .symbol(value.getSymbol())
                .name(value.getName())
                .enabled(value.getEnabled())
                .intervalMinutes(value.getIntervalMinutes())
                .tags(value.getTags() == null ? List.of() : value.getTags())
                .build();
    }

    default List<IndustryApplicationDTO.Target> toTargets(List<IndustryApplicationBO.Target> values) {
        return values == null ? List.of() : values.stream().map(this::toTarget).toList();
    }

    default IndustryApplicationBO.Config toConfigBO(IndustryApplicationDTO.Horizon horizon,
                                                    List<IndustryApplicationDTO.Target> items) {
        IndustryApplicationDTO.Horizon safeHorizon = horizon == null
                ? IndustryApplicationDTO.Horizon.builder().days(120).segments(4).build()
                : horizon;
        return IndustryApplicationBO.Config.builder()
                .version(1)
                .horizonDays(safeHorizon.getDays())
                .horizonSegments(safeHorizon.getSegments())
                .items(toTargetBOs(items))
                .build();
    }

    default IndustryApplicationBO.Target toTargetBO(IndustryApplicationDTO.Target value) {
        if (value == null) {
            return null;
        }
        return IndustryApplicationBO.Target.builder()
                .targetKey(value.getId())
                .targetType(value.getTargetType())
                .symbol(value.getSymbol())
                .name(value.getName())
                .enabled(value.getEnabled())
                .intervalMinutes(value.getIntervalMinutes())
                .tags(value.getTags() == null ? List.of() : value.getTags())
                .build();
    }

    default List<IndustryApplicationBO.Target> toTargetBOs(List<IndustryApplicationDTO.Target> values) {
        return values == null ? List.of() : values.stream().map(this::toTargetBO).toList();
    }

    default IndustryApplicationDTO.TargetCode toTargetCode(IndustryApplicationBO.TargetCode value) {
        if (value == null) {
            return null;
        }
        return IndustryApplicationDTO.TargetCode.builder()
                .code(value.getCode())
                .name(value.getName())
                .kind(value.getKind())
                .tdxFundFlowSupported(value.getTdxFundFlowSupported())
                .build();
    }

    default List<IndustryApplicationDTO.TargetCode> toTargetCodes(List<IndustryApplicationBO.TargetCode> values) {
        return values == null ? List.of() : values.stream().map(this::toTargetCode).toList();
    }

    default IndustryApplicationDTO.KlineResponse toKlineResponse(IndustryApplicationBO.Kline value) {
        if (value == null) {
            return IndustryApplicationDTO.KlineResponse.builder()
                    .kline(List.of())
                    .indicators(toIndicators(null))
                    .build();
        }
        return IndustryApplicationDTO.KlineResponse.builder()
                .targetType(value.getTargetType())
                .code(value.getCode())
                .name(value.getName())
                .period(value.getPeriod())
                .kline(toBars(value.getBars()))
                .indicators(toIndicators(value.getIndicators()))
                .fetchedAt(value.getFetchedAt())
                .source(value.getSource())
                .build();
    }

    default IndustryApplicationDTO.Bar toBar(IndustryApplicationBO.Bar value) {
        if (value == null) {
            return null;
        }
        return IndustryApplicationDTO.Bar.builder()
                .time(value.getTime())
                .open(value.getOpen())
                .high(value.getHigh())
                .low(value.getLow())
                .close(value.getClose())
                .prevClose(value.getPrevClose())
                .pct(value.getPct())
                .volumeLots(value.getVolumeLots())
                .amount(value.getAmount())
                .build();
    }

    default List<IndustryApplicationDTO.Bar> toBars(List<IndustryApplicationBO.Bar> values) {
        return values == null ? List.of() : values.stream().map(this::toBar).toList();
    }

    default IndustryApplicationDTO.Indicators toIndicators(IndustryApplicationBO.Indicators value) {
        if (value == null) {
            return IndustryApplicationDTO.Indicators.builder().barCount(0).build();
        }
        return IndustryApplicationDTO.Indicators.builder()
                .latestClose(value.getLatestClose())
                .latestPct(value.getLatestPct())
                .latestTime(value.getLatestTime())
                .ma20(value.getMa20())
                .ma60(value.getMa60())
                .ma120(value.getMa120())
                .ma250(value.getMa250())
                .aboveMa20(value.getAboveMa20())
                .aboveMa60(value.getAboveMa60())
                .aboveMa20Pct(value.getAboveMa20Pct())
                .aboveMa60Pct(value.getAboveMa60Pct())
                .aboveMa20Streak(value.getAboveMa20Streak())
                .high20(value.getHigh20())
                .low20(value.getLow20())
                .rangePos20(value.getRangePos20())
                .return5d(value.getReturn5d())
                .return20d(value.getReturn20d())
                .return60d(value.getReturn60d())
                .barCount(value.getBarCount())
                .build();
    }

    default IndustryApplicationDTO.ResultResponse toResultResponse(IndustryApplicationBO.Result value) {
        if (value == null) {
            return null;
        }
        return IndustryApplicationDTO.ResultResponse.builder()
                .target(toTarget(value.getTarget()))
                .updatedAt(value.getUpdatedAt())
                .kline(toBars(value.getBars()))
                .indicators(toIndicators(value.getIndicators()))
                .meta(IndustryApplicationDTO.ResultMeta.builder()
                        .source(value.getSource())
                        .period(value.getPeriod())
                        .count(value.getBars() == null ? 0 : value.getBars().size())
                        .build())
                .build();
    }

    default IndustryApplicationDTO.SectorOverviewItem toOverviewItem(IndustryApplicationBO.SectorOverviewItem value) {
        if (value == null) {
            return null;
        }
        return IndustryApplicationDTO.SectorOverviewItem.builder()
                .fullCode(value.getFullCode())
                .name(value.getName())
                .kind(value.getKind())
                .lastPrice(value.getLastPrice())
                .preClosePrice(value.getPreClosePrice())
                .openPrice(value.getOpenPrice())
                .highPrice(value.getHighPrice())
                .lowPrice(value.getLowPrice())
                .change(value.getChange())
                .changePct(value.getChangePct())
                .amplitudePct(value.getAmplitudePct())
                .highPct(value.getHighPct())
                .lowPct(value.getLowPct())
                .openPct(value.getOpenPct())
                .volume(value.getVolume())
                .amount(value.getAmount())
                .tradingDate(value.getTradingDate() == null ? null : value.getTradingDate().toString())
                .build();
    }

    default List<IndustryApplicationDTO.SectorOverviewItem> toOverviewItems(List<IndustryApplicationBO.SectorOverviewItem> values) {
        return values == null ? List.of() : values.stream().map(this::toOverviewItem).toList();
    }

    default IndustryApplicationDTO.FundFlowResponse toFundFlowResponse(IndustryApplicationBO.FundFlowSnapshot value) {
        if (value == null) {
            return IndustryApplicationDTO.FundFlowResponse.builder()
                    .ok(false)
                    .rowCount(0)
                    .totalPages(0)
                    .pageRowCounts(List.of())
                    .rows(List.of())
                    .error("no industry fund-flow snapshot found")
                    .build();
        }
        return IndustryApplicationDTO.FundFlowResponse.builder()
                .ok(value.getOk())
                .tradeDate(value.getTradeDate() == null ? null : value.getTradeDate().toString())
                .requestedTradeDate(value.getRequestedTradeDate() == null ? null : value.getRequestedTradeDate().toString())
                .fallbackTradeDate(value.getFallbackTradeDate())
                .servingMode(value.getServingMode())
                .rowCount(value.getRowCount())
                .totalPages(value.getTotalPages())
                .pageRowCounts(value.getPageRowCounts() == null ? List.of() : value.getPageRowCounts())
                .fetchedAt(value.getFetchedAt())
                .rows(toFundFlowRows(value.getRows()))
                .source(value.getSource())
                .sourceKind(value.getSourceKind())
                .stale(value.getStale())
                .staleReason(value.getStaleReason())
                .error(value.getError())
                .build();
    }

    default IndustryApplicationDTO.FundFlowHistoryResponse toFundFlowHistoryResponse(IndustryApplicationBO.FundFlowHistory value) {
        if (value == null) {
            return IndustryApplicationDTO.FundFlowHistoryResponse.builder()
                    .ok(false)
                    .days(0)
                    .dates(List.of())
                    .rowCount(0)
                    .rows(List.of())
                    .error("no industry fund-flow history found")
                    .build();
        }
        return IndustryApplicationDTO.FundFlowHistoryResponse.builder()
                .ok(value.getOk())
                .days(value.getDays())
                .dates(value.getDates() == null ? List.of() : value.getDates().stream().map(Object::toString).toList())
                .rowCount(value.getRowCount())
                .fetchedAt(value.getFetchedAt())
                .rows(toFundFlowRows(value.getRows()))
                .source(value.getSource())
                .error(value.getError())
                .build();
    }

    default IndustryApplicationDTO.FundFlowRow toFundFlowRow(IndustryApplicationBO.FundFlowRow value) {
        if (value == null) {
            return null;
        }
        return IndustryApplicationDTO.FundFlowRow.builder()
                .rank(value.getRank())
                .industry(value.getIndustry())
                .code(value.getCode())
                .changePct(value.getChangePct())
                .inflow(value.getInflow())
                .outflow(value.getOutflow())
                .net(value.getNet())
                .companyCount(value.getCompanyCount())
                .leaderStock(value.getLeaderStock())
                .leaderChange(value.getLeaderChange())
                .leaderPrice(value.getLeaderPrice())
                .tradeDate(value.getTradeDate() == null ? null : value.getTradeDate().toString())
                .source(value.getSource())
                .build();
    }

    default List<IndustryApplicationDTO.FundFlowRow> toFundFlowRows(List<IndustryApplicationBO.FundFlowRow> values) {
        return values == null ? List.of() : values.stream().map(this::toFundFlowRow).toList();
    }

    default IndustryApplicationDTO.TdxFundFlowResponse toTdxFundFlowResponse(IndustryApplicationBO.TdxFundFlowSnapshot value) {
        if (value == null) {
            return IndustryApplicationDTO.TdxFundFlowResponse.builder()
                    .applicable(false)
                    .ok(false)
                    .history(List.of())
                    .error("no tdx fund-flow snapshot found")
                    .build();
        }
        return IndustryApplicationDTO.TdxFundFlowResponse.builder()
                .applicable(value.getApplicable())
                .ok(value.getOk())
                .targetType(value.getTargetType())
                .symbol(value.getSymbol())
                .requestedTradeDate(value.getRequestedTradeDate() == null ? null : value.getRequestedTradeDate().toString())
                .tradeDate(value.getTradeDate() == null ? null : value.getTradeDate().toString())
                .code6(value.getCode6())
                .fullCode(value.getFullCode())
                .tdxName(value.getTdxName())
                .flowSectorName(value.getFlowSectorName())
                .seedCode(value.getSeedCode())
                .flowTradeDate(value.getFlowTradeDate() == null ? null : value.getFlowTradeDate().toString())
                .snapshotStatus(value.getSnapshotStatus())
                .selectionMode(value.getSelectionMode())
                .nameMatchType(value.getNameMatchType())
                .mainNet(value.getMainNet())
                .largeOrderNet(value.getLargeOrderNet())
                .mediumOrderNet(value.getMediumOrderNet())
                .smallOrderNet(value.getSmallOrderNet())
                .rowCount(value.getRowCount())
                .historyDays(value.getHistoryDays())
                .history(toTdxFundFlowPoints(value.getHistory()))
                .thsMappings(toTdxThsIndustryMappings(value.getThsMappings()))
                .constituents(toTdxFundFlowConstituents(value.getConstituents()))
                .source(value.getSource())
                .error(value.getError())
                .build();
    }

    default IndustryApplicationDTO.TdxFundFlowPoint toTdxFundFlowPoint(IndustryApplicationBO.TdxFundFlowPoint value) {
        if (value == null) {
            return null;
        }
        return IndustryApplicationDTO.TdxFundFlowPoint.builder()
                .tradeDate(value.getTradeDate() == null ? null : value.getTradeDate().toString())
                .flowTradeDate(value.getFlowTradeDate() == null ? null : value.getFlowTradeDate().toString())
                .snapshotStatus(value.getSnapshotStatus())
                .selectionMode(value.getSelectionMode())
                .mainNet(value.getMainNet())
                .largeOrderNet(value.getLargeOrderNet())
                .mediumOrderNet(value.getMediumOrderNet())
                .smallOrderNet(value.getSmallOrderNet())
                .build();
    }

    default List<IndustryApplicationDTO.TdxFundFlowPoint> toTdxFundFlowPoints(List<IndustryApplicationBO.TdxFundFlowPoint> values) {
        return values == null ? List.of() : values.stream().map(this::toTdxFundFlowPoint).toList();
    }

    default IndustryApplicationDTO.TdxThsIndustryMapping toTdxThsIndustryMapping(IndustryApplicationBO.TdxThsIndustryMapping value) {
        if (value == null) {
            return null;
        }
        return IndustryApplicationDTO.TdxThsIndustryMapping.builder()
                .tdxCode6(value.getTdxCode6())
                .tdxName(value.getTdxName())
                .thsCode(value.getThsCode())
                .thsName(value.getThsName())
                .matchType(value.getMatchType())
                .confidence(value.getConfidence())
                .priority(value.getPriority())
                .isPrimary(value.getPrimary())
                .remark(value.getRemark())
                .source(value.getSource())
                .build();
    }

    default List<IndustryApplicationDTO.TdxThsIndustryMapping> toTdxThsIndustryMappings(List<IndustryApplicationBO.TdxThsIndustryMapping> values) {
        return values == null ? List.of() : values.stream().map(this::toTdxThsIndustryMapping).toList();
    }

    default IndustryApplicationDTO.TdxFundFlowConstituent toTdxFundFlowConstituent(IndustryApplicationBO.TdxFundFlowConstituent value) {
        if (value == null) {
            return null;
        }
        return IndustryApplicationDTO.TdxFundFlowConstituent.builder()
                .code(value.getCode())
                .name(value.getName())
                .price(value.getPrice())
                .changePct(value.getChangePct())
                .turnoverPct(value.getTurnoverPct())
                .amountText(value.getAmountText())
                .sources(value.getSources() == null ? List.of() : value.getSources())
                .build();
    }

    default List<IndustryApplicationDTO.TdxFundFlowConstituent> toTdxFundFlowConstituents(List<IndustryApplicationBO.TdxFundFlowConstituent> values) {
        return values == null ? List.of() : values.stream().map(this::toTdxFundFlowConstituent).toList();
    }
}
