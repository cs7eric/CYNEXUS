package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.common.image.ImageMattingClient;
import cn.cccs7.cynexus.common.image.ImageMattingResult;
import cn.cccs7.cynexus.core.domain.food.FoodImageMattingPort;
import cn.cccs7.cynexus.core.domain.food.FoodImageMattingResult;
import org.springframework.stereotype.Component;

/** Adapts the shared image service to the core food domain gateway. */
@Component
public class HttpFoodImageMattingAdapter implements FoodImageMattingPort {

    private final ImageMattingClient imageMattingClient;

    public HttpFoodImageMattingAdapter(ImageMattingClient imageMattingClient) {
        this.imageMattingClient = imageMattingClient;
    }

    @Override
    public FoodImageMattingResult matte(byte[] content, String fileName, String contentType) {
        ImageMattingResult result = imageMattingClient.matte(content, fileName, contentType);
        return new FoodImageMattingResult(result.content(), result.contentType());
    }
}
