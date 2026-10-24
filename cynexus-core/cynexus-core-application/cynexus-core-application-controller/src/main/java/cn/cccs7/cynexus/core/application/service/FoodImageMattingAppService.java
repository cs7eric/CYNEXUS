package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.application.dto.FoodImageMattingResponse;
import cn.cccs7.cynexus.core.domain.food.FoodImageMattingDomainService;
import cn.cccs7.cynexus.core.domain.food.FoodImageMattingResult;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Orchestrates the food image matting request and maps the domain result to an HTTP-ready DTO. */
@Service
public class FoodImageMattingAppService {

    private static final long MAX_IMAGE_BYTES = 12L * 1024 * 1024;

    private final FoodImageMattingDomainService mattingDomainService;

    public FoodImageMattingAppService(FoodImageMattingDomainService mattingDomainService) {
        this.mattingDomainService = mattingDomainService;
    }

    public FoodImageMattingResponse matte(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "An image is required");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Choose an image smaller than 12 MB");
        }
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                fileName = "food-image.jpg";
            }
            FoodImageMattingResult result = mattingDomainService.matte(file.getBytes(), fileName, file.getContentType());
            return new FoodImageMattingResponse(result.content(), result.contentType());
        } catch (BizException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Unable to read the uploaded image");
        }
    }
}
