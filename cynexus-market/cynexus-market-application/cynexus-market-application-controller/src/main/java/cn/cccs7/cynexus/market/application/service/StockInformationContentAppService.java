package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.StockInformationContentDTO;
import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;
import cn.cccs7.cynexus.market.domain.information.StockInformationContentDomainService;
import org.springframework.stereotype.Service;

@Service
public class StockInformationContentAppService {

    private final StockInformationContentDomainService domainService;

    public StockInformationContentAppService(StockInformationContentDomainService domainService) {
        this.domainService = domainService;
    }

    public StockInformationContentDTO.ContentDTO content(String symbol, String kind, String source, String itemId, boolean fetchIfMissing) {
        return contentDto(domainService.content(symbol, kind, source, itemId, fetchIfMissing));
    }

    public StockInformationContentDTO.ContentDTO fetchContent(String symbol, String kind, String source, String itemId) {
        return contentDto(domainService.fetchContent(symbol, kind, source, itemId));
    }

    public StockInformationContentDTO.AnalysisDTO analysis(String symbol, String kind, String source, String itemId) {
        return analysisDto(domainService.analysis(symbol, kind, source, itemId));
    }

    public StockInformationContentDTO.AnalysisDTO runAnalysis(String symbol, String kind, String source, String itemId) {
        return analysisDto(domainService.runAnalysis(symbol, kind, source, itemId));
    }

    private StockInformationContentDTO.ContentDTO contentDto(StockInformationBO.Content value) {
        StockInformationBO.SourceRecord record = value.getSourceRecord();
        return StockInformationContentDTO.ContentDTO.builder().kind(record.getKind()).source(record.getSource()).itemId(record.getItemId())
                .title(value.getTitle()).sourceUrl(value.getSourceUrl()).resolvedUrl(value.getResolvedUrl()).contentType(value.getContentType())
                .status(value.getStatus()).text(value.getText()).contentHash(value.getContentHash()).message(value.getMessage())
                .extractedAt(value.getExtractedAt()).build();
    }

    private StockInformationContentDTO.AnalysisDTO analysisDto(StockInformationBO.Analysis value) {
        return StockInformationContentDTO.AnalysisDTO.builder().status(value.getStatus()).resultJson(value.getResultJson())
                .modelCode(value.getModelCode()).errorMessage(value.getErrorMessage()).analyzedAt(value.getAnalyzedAt()).build();
    }

}
