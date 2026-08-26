package cn.cccs7.cynexus.core.domain.mediatranscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.cccs7.cynexus.common.core.exception.BizException;
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
    void extractsShareTextUrl() {
        assertThat(policy.normalizeUrl(
                "9.25 复制打开抖音，看看【马克fo的作品】我们该怎么做出有质量的交易决策？ # 交易 # 过... https://v.douyin.com/ZXOzFdRxmc4/ 12/17 CHv:/ S@L.jP :1pm"))
                .isEqualTo("https://v.douyin.com/ZXOzFdRxmc4/");
    }

    @Test
    void keepsDirectMediaUrlWithoutResolvedLink() {
        assertThat(policy.selectDownloadUrl(MediaTranscriptionResolvedLinkBO.builder().build(),
                " https://cdn.example/demo.wav "))
                .isEqualTo("https://cdn.example/demo.wav");
    }

    @Test
    void doesNotSendPageUrlToTranscriber() {
        assertThat(policy.selectDownloadUrl(MediaTranscriptionResolvedLinkBO.builder().build(),
                "https://v.douyin.com/ZXOzFdRxmc4/"))
                .isEmpty();
        assertThatThrownBy(() -> policy.requireResolvedMediaUrl(null, "https://v.douyin.com/ZXOzFdRxmc4/"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未拿到视频或音频地址");
    }
}
