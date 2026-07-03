package cn.cccs7.cynexus.core.domain.service.impl;

import cn.cccs7.cynexus.core.domain.client.MediaTranscriptionWorkerClient;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionAskResultBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionBinaryBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionRemoteParseRequestBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionStreamBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskRefBO;
import cn.cccs7.cynexus.core.domain.service.MediaTranscriptionTaskDomainService;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class MediaTranscriptionTaskDomainServiceImpl implements MediaTranscriptionTaskDomainService {

    private final MediaTranscriptionWorkerClient mediaTranscriptionWorkerClient;

    public MediaTranscriptionTaskDomainServiceImpl(MediaTranscriptionWorkerClient mediaTranscriptionWorkerClient) {
        this.mediaTranscriptionWorkerClient = mediaTranscriptionWorkerClient;
    }

    @Override
    public MediaTranscriptionTaskRefBO upload(Path filePath, String fileName, String contentType) {
        return mediaTranscriptionWorkerClient.upload(filePath, fileName, contentType);
    }

    @Override
    public MediaTranscriptionTaskRefBO remoteParse(MediaTranscriptionRemoteParseRequestBO request) {
        return mediaTranscriptionWorkerClient.remoteParse(request);
    }

    @Override
    public MediaTranscriptionTaskBO getTask(String taskId) {
        return mediaTranscriptionWorkerClient.getTask(taskId);
    }

    @Override
    public MediaTranscriptionAskResultBO ask(String taskId, String question) {
        return mediaTranscriptionWorkerClient.ask(taskId, question);
    }

    @Override
    public MediaTranscriptionBinaryBO exportMarkdown(String taskId) {
        return mediaTranscriptionWorkerClient.exportMarkdown(taskId);
    }

    @Override
    public MediaTranscriptionStreamBO openStream(String taskId) {
        return mediaTranscriptionWorkerClient.openStream(taskId);
    }
}
