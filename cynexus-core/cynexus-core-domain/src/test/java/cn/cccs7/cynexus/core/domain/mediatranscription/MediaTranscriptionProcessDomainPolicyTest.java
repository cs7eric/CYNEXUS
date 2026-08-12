package cn.cccs7.cynexus.core.domain.mediatranscription;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionResolvedLinkBO;
import org.junit.jupiter.api.Test;

class MediaTranscriptionProcessDomainPolicyTest {

    private final MediaTranscriptionProcessDomainPolicy policy = new MediaTranscriptionProcessDomainPolicy();

    @Test
    void followsTheUrlPasteMediaPriority() {
        MediaTranscriptionResolvedLinkBO resolved = MediaTranscriptionResolvedLinkBO.builder()
                .originDownloadAudioUrl(" https://cdn.example/audio.mp3 ")
                .originDownloadVideoUrl("https://cdn.example/origin.mp4")
                .downloadVideoUrl("https://cdn.example/proxy.mp4")
                .downloadAudioUrl("https://cdn.example/proxy.mp3")
                .build();

        assertThat(policy.selectDownloadUrl(resolved, "https://example.com/page"))
                .isEqualTo("https://cdn.example/audio.mp3");
    }

    @Test
    void fallsBackToSourceUrlWithoutResolvedMedia() {
        assertThat(policy.selectDownloadUrl(MediaTranscriptionResolvedLinkBO.builder().build(), " https://example.com/page "))
                .isEqualTo("https://example.com/page");
    }
}
