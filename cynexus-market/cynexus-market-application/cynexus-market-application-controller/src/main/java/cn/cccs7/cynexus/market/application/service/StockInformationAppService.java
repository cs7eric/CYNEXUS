package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.StockInformationDTO;
import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;
import cn.cccs7.cynexus.market.domain.repository.StockInformationRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockInformationAppService {

    private final StockInformationRepository stockInformationRepository;

    public StockInformationAppService(StockInformationRepository stockInformationRepository) {
        this.stockInformationRepository = stockInformationRepository;
    }

    public StockInformationDTO.FeedDTO feed(String symbol, int limit) {
        StockInformationBO.Feed feed = stockInformationRepository.feed(symbol, Math.max(1, Math.min(limit, 100)));
        return StockInformationDTO.FeedDTO.builder()
                .symbol(feed.getSymbol())
                .queriedAt(feed.getQueriedAt())
                .announcements(safe(feed.getAnnouncements()).stream().map(this::announcement).toList())
                .news(safe(feed.getNews()).stream().map(this::news).toList())
                .research(safe(feed.getResearch()).stream().map(this::research).toList())
                .investorInteractions(safe(feed.getInvestorInteractions()).stream().map(this::interaction).toList())
                .roadshows(safe(feed.getRoadshows()).stream().map(this::roadshow).toList())
                .build();
    }

    private StockInformationDTO.AnnouncementDTO announcement(StockInformationBO.Announcement value) {
        return StockInformationDTO.AnnouncementDTO.builder()
                .id(value.getId()).title(value.getTitle()).category(value.getCategory()).publishedAt(value.getPublishedAt())
                .source(value.getSource()).url(value.getUrl()).build();
    }

    private StockInformationDTO.NewsDTO news(StockInformationBO.News value) {
        return StockInformationDTO.NewsDTO.builder()
                .id(value.getId()).title(value.getTitle()).summary(value.getSummary()).mediaName(value.getMediaName())
                .publishedAt(value.getPublishedAt()).source(value.getSource()).url(value.getUrl()).build();
    }

    private StockInformationDTO.ResearchDTO research(StockInformationBO.Research value) {
        return StockInformationDTO.ResearchDTO.builder()
                .id(value.getId()).title(value.getTitle()).organization(value.getOrganization()).author(value.getAuthor())
                .rating(value.getRating()).publishedAt(value.getPublishedAt()).source(value.getSource()).url(value.getUrl()).build();
    }

    private StockInformationDTO.InvestorInteractionDTO interaction(StockInformationBO.InvestorInteraction value) {
        return StockInformationDTO.InvestorInteractionDTO.builder()
                .id(value.getId()).question(value.getQuestion()).answer(value.getAnswer()).answerer(value.getAnswerer())
                .askedAt(value.getAskedAt()).source(value.getSource()).build();
    }

    private StockInformationDTO.RoadshowDTO roadshow(StockInformationBO.Roadshow value) {
        return StockInformationDTO.RoadshowDTO.builder()
                .title(value.getTitle()).type(value.getType()).startAt(value.getStartAt())
                .source(value.getSource()).url(value.getUrl()).build();
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
