package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.market.api.dto.StockInformationContentDTO;
import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;
import cn.cccs7.cynexus.market.domain.repository.StockInformationRepository;
import cn.cccs7.cynexus.market.infra.client.HttpStockInformationAiClient;
import cn.cccs7.cynexus.market.infra.client.HttpStockInformationContentDataClient;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class StockInformationContentAppService {

    private static final String ANALYSIS_SCHEMA_VERSION = "stock_information_v2";
    private static final String PROMPT_VERSION = "stock_information_v2";

    private final StockInformationRepository repository;
    private final HttpStockInformationContentDataClient dataClient;
    private final HttpStockInformationAiClient aiClient;

    public StockInformationContentAppService(StockInformationRepository repository,
                                             HttpStockInformationContentDataClient dataClient,
                                             HttpStockInformationAiClient aiClient) {
        this.repository = repository;
        this.dataClient = dataClient;
        this.aiClient = aiClient;
    }

    public StockInformationContentDTO.ContentDTO content(String symbol, String kind, String source, String itemId, boolean fetchIfMissing) {
        StockInformationBO.SourceRecord record = requireRecord(symbol, kind, source, itemId);
        StockInformationBO.Content existing = repository.findContent(record);
        if (existing == null && fetchIfMissing) {
            existing = fetch(record);
        }
        return contentDto(existing == null ? pending(record) : existing);
    }

    public StockInformationContentDTO.ContentDTO fetchContent(String symbol, String kind, String source, String itemId) {
        return contentDto(fetch(requireRecord(symbol, kind, source, itemId)));
    }

    public StockInformationContentDTO.AnalysisDTO analysis(String symbol, String kind, String source, String itemId) {
        StockInformationBO.SourceRecord record = requireRecord(symbol, kind, source, itemId);
        StockInformationBO.Content content = repository.findContent(record);
        if (content == null || !"ready".equals(content.getStatus()) || blank(content.getContentHash())) {
            return StockInformationContentDTO.AnalysisDTO.builder().status("content_required").build();
        }
        StockInformationBO.Analysis analysis = repository.findLatestAnalysis(content.getId(), content.getContentHash(), ANALYSIS_SCHEMA_VERSION, PROMPT_VERSION);
        return analysisDto(analysis == null ? StockInformationBO.Analysis.builder().status("not_requested").build() : analysis);
    }

    public StockInformationContentDTO.AnalysisDTO runAnalysis(String symbol, String kind, String source, String itemId) {
        StockInformationBO.SourceRecord record = requireRecord(symbol, kind, source, itemId);
        StockInformationBO.Content content = repository.findContent(record);
        if (content == null || !"ready".equals(content.getStatus()) || blank(content.getText()) || blank(content.getContentHash())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Readable content is required before AI analysis.");
        }
        StockInformationBO.Analysis cached = repository.findLatestAnalysis(content.getId(), content.getContentHash(), ANALYSIS_SCHEMA_VERSION, PROMPT_VERSION);
        if (cached != null && "ready".equals(cached.getStatus())) {
            return analysisDto(cached);
        }
        try {
            HttpStockInformationAiClient.AnalysisResponse response = aiClient.analyze(content.getSourceRecord().getCode(), content.getTitle(), content.getText());
            return analysisDto(repository.saveAnalysis(StockInformationBO.Analysis.builder()
                    .contentId(content.getId()).contentHash(content.getContentHash()).schemaVersion(ANALYSIS_SCHEMA_VERSION)
                    .promptVersion(PROMPT_VERSION).modelCode(response.modelCode()).status("ready")
                    .resultJson(response.resultJson()).analyzedAt(OffsetDateTime.now()).build()));
        } catch (RuntimeException exception) {
            repository.saveAnalysis(StockInformationBO.Analysis.builder()
                    .contentId(content.getId()).contentHash(content.getContentHash()).schemaVersion(ANALYSIS_SCHEMA_VERSION)
                    .promptVersion(PROMPT_VERSION).modelCode("").status("failed").errorMessage(exception.getMessage())
                    .analyzedAt(OffsetDateTime.now()).build());
            throw exception;
        }
    }

    private StockInformationBO.Content fetch(StockInformationBO.SourceRecord record) {
        return repository.saveContent(dataClient.extract(record));
    }

    private StockInformationBO.SourceRecord requireRecord(String symbol, String kind, String source, String itemId) {
        StockInformationBO.SourceRecord record = repository.findSourceRecord(symbol, kind, source, itemId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Information item was not found.");
        }
        return record;
    }

    private StockInformationBO.Content pending(StockInformationBO.SourceRecord record) {
        return StockInformationBO.Content.builder().sourceRecord(record).title(record.getTitle()).sourceUrl(record.getUrl()).status("not_fetched").build();
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

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
