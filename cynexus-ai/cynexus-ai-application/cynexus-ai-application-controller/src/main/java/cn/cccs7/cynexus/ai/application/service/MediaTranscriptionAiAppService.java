package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.MediaTranscriptionAskDTO;
import cn.cccs7.cynexus.ai.api.dto.MediaTranscriptionMetadataDTO;
import cn.cccs7.cynexus.ai.api.dto.MediaTranscriptionTextDTO;
import cn.cccs7.cynexus.ai.api.dto.MediaTranscriptionEnrichmentDTO;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionAskRequest;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionMetadataRequest;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionPolishRequest;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionSummaryRequest;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionEnrichmentRequest;
import cn.cccs7.cynexus.ai.domain.entity.MediaTranscriptionEnrichmentBO;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionEnrichmentCommand;
import cn.cccs7.cynexus.ai.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionDomainService;
import org.springframework.stereotype.Service;

@Service
public class MediaTranscriptionAiAppService {

    private final MediaTranscriptionDomainService domainService;

    public MediaTranscriptionAiAppService(MediaTranscriptionDomainService domainService) {
        this.domainService = domainService;
    }

    public MediaTranscriptionTextDTO polish(MediaTranscriptionPolishRequest request) {
        return MediaTranscriptionTextDTO.builder().text(domainService.polish(new MediaTranscriptionDomainService.TextCommand(
                request == null ? null : request.getText(), request == null ? null : request.getPreferredModelCode()))).build();
    }

    public MediaTranscriptionEnrichmentDTO enrich(MediaTranscriptionEnrichmentRequest request) {
        return enrich(request, null, null);
    }

    public MediaTranscriptionEnrichmentDTO enrich(MediaTranscriptionEnrichmentRequest request, String tenantKey, String principalKey) {
        MediaTranscriptionEnrichmentBO result = domainService.enrich(new MediaTranscriptionEnrichmentCommand(
                request == null ? null : request.getTranscript(), request == null ? null : request.getTitle(),
                request == null ? null : request.getPlatform(), request == null ? null : request.getDuration(),
                request == null ? null : request.getPreferredModelCode(), tenantKey, principalKey));
        return MediaTranscriptionEnrichmentDTO.builder().polishedText(result.polishedText()).summary(result.summary())
                .shortSummary(result.shortSummary()).keyPoints(result.keyPoints()).topics(result.topics())
                .methodology(result.methodology()).actionItems(result.actionItems()).risks(result.risks())
                .title(result.title()).categories(result.categories()).tags(result.tags()).build();
    }

    public MediaTranscriptionTextDTO summary(MediaTranscriptionSummaryRequest request) {
        return MediaTranscriptionTextDTO.builder().text(domainService.summary(new MediaTranscriptionDomainService.SummaryCommand(
                request == null ? null : request.getText(), request == null ? null : request.getTitle(),
                request == null ? null : request.getPlatform(), request == null ? null : request.getDuration(),
                request == null ? null : request.getPreferredModelCode()))).build();
    }

    public MediaTranscriptionMetadataDTO metadata(MediaTranscriptionMetadataRequest request) {
        MediaTranscriptionMetadataBO metadata = domainService.metadata(new MediaTranscriptionDomainService.MetadataCommand(
                request == null ? null : request.getPolishedText(), request == null ? null : request.getSummaryText(),
                request == null ? null : request.getPreferredModelCode()));
        return MediaTranscriptionMetadataDTO.builder().title(metadata.title()).categories(metadata.categories()).tags(metadata.tags()).build();
    }

    public MediaTranscriptionAskDTO ask(MediaTranscriptionAskRequest request) {
        return MediaTranscriptionAskDTO.builder().answer(domainService.ask(new MediaTranscriptionDomainService.AskCommand(
                request == null ? null : request.getPolishedText(), request == null ? null : request.getSummaryText(),
                request == null ? null : request.getQuestion(), request == null ? null : request.getPreferredModelCode()))).build();
    }
}
