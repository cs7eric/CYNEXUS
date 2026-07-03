package cn.cccs7.cynexus.core.domain.client;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionAskResultBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionBinaryBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionRemoteParseRequestBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionStreamBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskRefBO;
import java.nio.file.Path;

public interface MediaTranscriptionWorkerClient {

    MediaTranscriptionTaskRefBO upload(Path filePath, String fileName, String contentType);

    MediaTranscriptionTaskRefBO remoteParse(MediaTranscriptionRemoteParseRequestBO request);

    MediaTranscriptionTaskBO getTask(String taskId);

    MediaTranscriptionAskResultBO ask(String taskId, String question);

    MediaTranscriptionBinaryBO exportMarkdown(String taskId);

    MediaTranscriptionStreamBO openStream(String taskId);
}
