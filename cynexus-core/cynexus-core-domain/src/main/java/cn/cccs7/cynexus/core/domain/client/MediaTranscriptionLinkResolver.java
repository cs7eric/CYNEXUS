package cn.cccs7.cynexus.core.domain.client;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionResolvedLinkBO;

public interface MediaTranscriptionLinkResolver {

    MediaTranscriptionResolvedLinkBO resolve(String sourceUrl);
}
