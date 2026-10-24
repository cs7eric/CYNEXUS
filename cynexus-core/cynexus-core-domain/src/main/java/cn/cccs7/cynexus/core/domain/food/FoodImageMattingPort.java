package cn.cccs7.cynexus.core.domain.food;

/** Domain boundary for producing a reusable food image with its background removed. */
public interface FoodImageMattingPort {

    FoodImageMattingResult matte(byte[] content, String fileName, String contentType);
}
