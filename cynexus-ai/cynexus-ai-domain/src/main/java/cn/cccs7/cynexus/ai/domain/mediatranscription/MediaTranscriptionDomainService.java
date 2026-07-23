package cn.cccs7.cynexus.ai.domain.mediatranscription;

import cn.cccs7.cynexus.ai.domain.entity.MediaTranscriptionMetadataBO;

public interface MediaTranscriptionDomainService {

    String polish(TextCommand command);

    String summary(SummaryCommand command);

    MediaTranscriptionMetadataBO metadata(MetadataCommand command);

    String ask(AskCommand command);

    record TextCommand(String text, String preferredModelCode) { }

    record SummaryCommand(String text, String title, String platform, Integer duration,
                          String preferredModelCode) { }

    record MetadataCommand(String polishedText, String summaryText, String preferredModelCode) { }

    record AskCommand(String polishedText, String summaryText, String question,
                      String preferredModelCode) { }
}
