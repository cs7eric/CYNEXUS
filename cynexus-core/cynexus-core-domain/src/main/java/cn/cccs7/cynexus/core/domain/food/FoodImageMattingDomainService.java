package cn.cccs7.cynexus.core.domain.food;

import org.springframework.stereotype.Service;

/** Domain-facing entry point for processing a food image before it is mapped as an asset. */
@Service
public class FoodImageMattingDomainService {

    private final FoodImageMattingPort mattingPort;

    public FoodImageMattingDomainService(FoodImageMattingPort mattingPort) {
        this.mattingPort = mattingPort;
    }

    public FoodImageMattingResult matte(byte[] content, String fileName, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Image content is required");
        }
        return mattingPort.matte(content.clone(), fileName, contentType);
    }
}
