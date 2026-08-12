package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.PortfolioReviewAnchorDTO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioIdGenerator;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioReviewAnchorBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioReviewAnchorRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PortfolioReviewAnchorAppService {
    private static final String PRINCIPAL = "workspace-user";
    private static final String TENANT = "default";

    private final PortfolioReviewAnchorRepository repository;
    private final PortfolioIdGenerator idGenerator;

    public PortfolioReviewAnchorAppService(PortfolioReviewAnchorRepository repository, PortfolioIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    public List<PortfolioReviewAnchorDTO> list(String symbol, LocalDate tradeDate) {
        return repository.list(PRINCIPAL, TENANT, symbol, tradeDate).stream().map(this::toDto).toList();
    }

    public PortfolioReviewAnchorDTO save(PortfolioReviewAnchorDTO dto) {
        PortfolioReviewAnchorBO value = new PortfolioReviewAnchorBO(
                dto.getId() == null ? idGenerator.nextId() : dto.getId(), PRINCIPAL, TENANT, dto.getSymbol(),
                dto.getInstrumentType(), dto.getTradeId(), dto.getReviewId(), dto.getAnchorType(), dto.getSide(),
                dto.getOccurredAt(), dto.getPrice(), dto.getNote(), dto.getTags());
        if (dto.getId() == null) repository.create(value);
        else if (!repository.update(value)) throw new IllegalStateException("portfolio review anchor was not updated");
        return toDto(value);
    }

    public void delete(long id) {
        if (!repository.delete(PRINCIPAL, TENANT, id)) throw new IllegalArgumentException("portfolio review anchor not found");
    }

    private PortfolioReviewAnchorDTO toDto(PortfolioReviewAnchorBO value) {
        PortfolioReviewAnchorDTO dto = new PortfolioReviewAnchorDTO();
        dto.setId(value.id());
        dto.setSymbol(value.symbol());
        dto.setInstrumentType(value.instrumentType());
        dto.setTradeId(value.tradeId());
        dto.setReviewId(value.reviewId());
        dto.setAnchorType(value.anchorType());
        dto.setSide(value.side());
        dto.setOccurredAt(value.occurredAt());
        dto.setPrice(value.price());
        dto.setNote(value.note());
        dto.setTags(value.tags());
        return dto;
    }
}
