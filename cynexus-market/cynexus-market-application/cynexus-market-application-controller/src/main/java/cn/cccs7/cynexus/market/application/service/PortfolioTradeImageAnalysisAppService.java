package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.PortfolioTradeImageAnalysisDTO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeImageAnalysisBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeImageAnalysisDomainService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PortfolioTradeImageAnalysisAppService {
    private final PortfolioTradeImageAnalysisDomainService domainService;

    public PortfolioTradeImageAnalysisAppService(PortfolioTradeImageAnalysisDomainService domainService) {
        this.domainService = domainService;
    }

    public PortfolioTradeImageAnalysisDTO analyze(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("trade screenshot is required");
        }
        try {
            PortfolioTradeImageAnalysisBO result = domainService.analyze(
                    file.getOriginalFilename(), file.getContentType(), file.getBytes());
            return PortfolioTradeImageAnalysisDTO.builder()
                    .scenarioCode(result.scenarioCode())
                    .status(result.status())
                    .content(result.content().toString())
                    .build();
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("failed to read trade screenshot", exception);
        }
    }
}
