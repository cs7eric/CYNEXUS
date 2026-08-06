package cn.cccs7.cynexus.wechat.infra.client;

import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.api.feign.AiExecutionFeignClient;
import cn.cccs7.cynexus.ai.api.request.AiVisionExecuteRequest;
import cn.cccs7.cynexus.ai.api.request.AiVisionImageRequest;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.wechat.domain.gateway.WechatVisionGateway;
import cn.cccs7.cynexus.wechat.domain.model.WechatStoredImage;
import cn.cccs7.cynexus.wechat.domain.model.WechatVisionResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class WechatAiVisionClient implements WechatVisionGateway {
    private final AiExecutionFeignClient aiExecutionFeignClient;

    public WechatAiVisionClient(AiExecutionFeignClient aiExecutionFeignClient) {
        this.aiExecutionFeignClient = aiExecutionFeignClient;
    }

    @Override
    public WechatVisionResult classify(WechatStoredImage image) {
        try {
            Path path = Path.of(image.storagePath()).toAbsolutePath().normalize();
            byte[] bytes = Files.readAllBytes(path);
            AiVisionImageRequest imageRequest = new AiVisionImageRequest();
            imageRequest.setFileName(path.getFileName().toString());
            imageRequest.setContentType(image.contentType());
            imageRequest.setDataUrl("data:" + image.contentType() + ";base64," + Base64.getEncoder().encodeToString(bytes));

            AiVisionExecuteRequest request = new AiVisionExecuteRequest();
            request.setScenarioCode("wechat_image_route");
            request.setUserInput("这是一张来自微信公众号的图片，只需要判断后续处理路径。");
            request.setImages(java.util.List.of(imageRequest));
            Result<AiExecuteDTO> response = aiExecutionFeignClient.executeVision(request);
            if (response == null || response.isFailed() || response.getData() == null) {
                throw new IllegalStateException(response == null ? "AI 服务无响应" : response.getMessage());
            }
            AiExecuteDTO data = response.getData();
            return new WechatVisionResult(data.getExecutionNo(), data.getStatus(), data.getModelCode(), data.getContent());
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "微信图片分类调用失败: " + exception.getMessage());
        }
    }
}
