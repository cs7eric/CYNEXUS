package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.PortfolioReviewDTO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioIdGenerator;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioReviewBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioReviewRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PortfolioReviewAppService {
    private static final String PRINCIPAL = "workspace-user";
    private static final String TENANT = "default";
    private final PortfolioReviewRepository repository;
    private final PortfolioIdGenerator idGenerator;

    public PortfolioReviewAppService(PortfolioReviewRepository repository, PortfolioIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    public List<PortfolioReviewDTO> list(String symbol) {
        return repository.list(PRINCIPAL, TENANT, symbol).stream().map(this::toDto).toList();
    }

    public PortfolioReviewDTO get(long id) {
        return repository.findById(PRINCIPAL, TENANT, id).map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("portfolio review not found"));
    }

    public PortfolioReviewDTO save(PortfolioReviewDTO dto) {
        PortfolioReviewBO value = new PortfolioReviewBO(dto.getId() == null ? idGenerator.nextId() : dto.getId(),
                PRINCIPAL, TENANT, dto.getSymbol(), dto.getPeriodStart(), dto.getPeriodEnd(), dto.getThesis(),
                dto.getPlan(), dto.getExecution(), dto.getLessons(), dto.getNextAction(), dto.getTags(), dto.getRating(),
                dto.getMetricsSnapshot(), dto.getStatus() == null ? "DRAFT" : dto.getStatus());
        if (dto.getId() == null) repository.create(value); else if (!repository.update(value)) throw new IllegalStateException("portfolio review was not updated");
        return toDto(value);
    }

    private PortfolioReviewDTO toDto(PortfolioReviewBO value) {
        PortfolioReviewDTO dto = new PortfolioReviewDTO();
        dto.setId(value.id()); dto.setSymbol(value.symbol()); dto.setPeriodStart(value.periodStart()); dto.setPeriodEnd(value.periodEnd());
        dto.setThesis(value.thesis()); dto.setPlan(value.plan()); dto.setExecution(value.execution()); dto.setLessons(value.lessons());
        dto.setNextAction(value.nextAction()); dto.setTags(value.tags()); dto.setRating(value.rating()); dto.setMetricsSnapshot(value.metricsSnapshot()); dto.setStatus(value.status());
        return dto;
    }
}
