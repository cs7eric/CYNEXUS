package cn.cccs7.cynexus.market.domain.information.impl;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;
import cn.cccs7.cynexus.market.domain.repository.StockInformationRepository;
import cn.cccs7.cynexus.market.domain.information.StockInformationAnalysisGateway;
import cn.cccs7.cynexus.market.domain.information.StockInformationContentDomainService;
import cn.cccs7.cynexus.market.domain.information.StockInformationContentExtractor;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class StockInformationContentDomainServiceImpl implements StockInformationContentDomainService {

    private static final String ANALYSIS_SCHEMA_VERSION = "stock_information_v2";
    private static final String PROMPT_VERSION = "stock_information_v2";

    private final StockInformationRepository repository;
    private final StockInformationContentExtractor contentExtractor;
    private final StockInformationAnalysisGateway analysisGateway;

    public StockInformationContentDomainServiceImpl(StockInformationRepository repository,
                                                    StockInformationContentExtractor contentExtractor,
                                                    StockInformationAnalysisGateway analysisGateway) {
        this.repository = repository;
        this.contentExtractor = contentExtractor;
        this.analysisGateway = analysisGateway;
    }

    @Override
    public StockInformationBO.Content content(String symbol, String kind, String source, String itemId,
                                              boolean fetchIfMissing) {
        StockInformationBO.SourceRecord record = requireRecord(symbol, kind, source, itemId);
        StockInformationBO.Content existing = repository.findContent(record);
        if (existing == null && fetchIfMissing) {
            existing = fetch(record);
        }
        return existing == null ? pending(record) : existing;
    }

    @Override
    public StockInformationBO.Content fetchContent(String symbol, String kind, String source, String itemId) {
        return fetch(requireRecord(symbol, kind, source, itemId));
    }

    @Override
    public StockInformationBO.Analysis analysis(String symbol, String kind, String source, String itemId) {
        StockInformationBO.SourceRecord record = requireRecord(symbol, kind, source, itemId);
        StockInformationBO.Content content = repository.findContent(record);
        if (content == null || !"ready".equals(content.getStatus()) || blank(content.getContentHash())) {
            return StockInformationBO.Analysis.builder().status("content_required").build();
        }
        StockInformationBO.Analysis analysis = repository.findLatestAnalysis(
                content.getId(), content.getContentHash(), ANALYSIS_SCHEMA_VERSION, PROMPT_VERSION);
        return analysis == null ? StockInformationBO.Analysis.builder().status("not_requested").build() : analysis;
    }

    @Override
    public StockInformationBO.Analysis runAnalysis(String symbol, String kind, String source, String itemId) {
        StockInformationBO.SourceRecord record = requireRecord(symbol, kind, source, itemId);
        StockInformationBO.Content content = repository.findContent(record);
        if (content == null || !"ready".equals(content.getStatus()) || blank(content.getText())
                || blank(content.getContentHash())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Readable content is required before AI analysis.");
        }
        StockInformationBO.Analysis cached = repository.findLatestAnalysis(
                content.getId(), content.getContentHash(), ANALYSIS_SCHEMA_VERSION, PROMPT_VERSION);
        if (cached != null && "ready".equals(cached.getStatus())) {
            return cached;
        }
        try {
            StockInformationAnalysisGateway.AnalysisResult response = analysisGateway.analyze(
                    content.getSourceRecord().getCode(), content.getTitle(), content.getText());
            return repository.saveAnalysis(StockInformationBO.Analysis.builder()
                    .contentId(content.getId()).contentHash(content.getContentHash())
                    .schemaVersion(ANALYSIS_SCHEMA_VERSION).promptVersion(PROMPT_VERSION)
                    .modelCode(response.modelCode()).status("ready").resultJson(response.resultJson())
                    .analyzedAt(OffsetDateTime.now()).build());
        } catch (RuntimeException exception) {
            repository.saveAnalysis(StockInformationBO.Analysis.builder()
                    .contentId(content.getId()).contentHash(content.getContentHash())
                    .schemaVersion(ANALYSIS_SCHEMA_VERSION).promptVersion(PROMPT_VERSION).modelCode("")
                    .status("failed").errorMessage(exception.getMessage()).analyzedAt(OffsetDateTime.now()).build());
            throw exception;
        }
    }

    private StockInformationBO.Content fetch(StockInformationBO.SourceRecord record) {
        return repository.saveContent(contentExtractor.extract(record));
    }

    private StockInformationBO.SourceRecord requireRecord(String symbol, String kind, String source, String itemId) {
        StockInformationBO.SourceRecord record = repository.findSourceRecord(symbol, kind, source, itemId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Information item was not found.");
        }
        return record;
    }

    private StockInformationBO.Content pending(StockInformationBO.SourceRecord record) {
        return StockInformationBO.Content.builder().sourceRecord(record).title(record.getTitle())
                .sourceUrl(record.getUrl()).status("not_fetched").build();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
