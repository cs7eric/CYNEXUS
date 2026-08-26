package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.MarketEventReplayDTO;
import cn.cccs7.cynexus.market.domain.factorresearch.EventReplayBO;
import cn.cccs7.cynexus.market.domain.factorresearch.EventReplayDomainService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class EventReplayAppService {
    private final EventReplayDomainService domainService;
    public EventReplayAppService(EventReplayDomainService domainService) { this.domainService = domainService; }

    public MarketEventReplayDTO replay(String code, LocalDate tradeDate, OffsetDateTime anchorAt, Integer minuteLimit) {
        EventReplayBO.Result result = domainService.replay(new EventReplayBO.Query(code, tradeDate, anchorAt, minuteLimit == null ? 240 : minuteLimit));
        var replay = result.marketReplay();
        return MarketEventReplayDTO.builder().code(result.code()).tradeDate(result.tradeDate()).anchorAt(result.anchorAt())
                .status(result.status()).source(result.source()).dataCutoffAt(result.dataCutoffAt())
                .coverage(MarketEventReplayDTO.Coverage.builder().stockBarsRead(result.coverage().stockBarsRead())
                        .stockBarsIncluded(result.coverage().stockBarsIncluded()).marketAvailable(result.coverage().marketAvailable())
                        .industryAvailable(result.coverage().industryAvailable()).stockAvailable(result.coverage().stockAvailable()).build())
                .warnings(result.warnings())
                .stockMinuteBars(result.stockMinuteBars().stream().map(bar -> MarketEventReplayDTO.Bar.builder()
                        .timestamp(bar.getTimestamp()).tradeDate(bar.getTradeDate()).open(bar.getOpen()).high(bar.getHigh()).low(bar.getLow())
                        .close(bar.getClose()).volume(bar.getVolume()).turnover(bar.getTurnover()).build()).toList())
                .marketReplay(MarketEventReplayDTO.MarketReplay.builder().dataStatus(replay.getDataStatus()).dataTradeDate(replay.getDataTradeDate())
                        .source(replay.getSource()).requestedFrom(replay.getRequestedFrom()).requestedTo(replay.getRequestedTo())
                        .eventCount(replay.getEvents() == null ? 0 : replay.getEvents().size())
                        .industryPointCount(replay.getIndustryPoints() == null ? 0 : replay.getIndustryPoints().size()).message(replay.getMessage()).build()).build();
    }
}
