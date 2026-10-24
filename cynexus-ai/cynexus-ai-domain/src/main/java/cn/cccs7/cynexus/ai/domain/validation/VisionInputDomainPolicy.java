package cn.cccs7.cynexus.ai.domain.validation;

import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Input bounds for image-based AI capabilities. */
public final class VisionInputDomainPolicy {
    private static final String FOOD_NUTRITION_LABEL_PARSE = "food_nutrition_label_parse";
    private static final int MAX_FOOD_LABEL_DATA_URL_LENGTH = 3_600_000;
    private static final Set<String> FOOD_LABEL_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private VisionInputDomainPolicy() { }

    public static void validate(String capability, List<AiImageInputBO> images) {
        if (!FOOD_NUTRITION_LABEL_PARSE.equals(normalize(capability))) return;
        if (images == null || images.isEmpty() || images.size() > 2) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "Nutrition label recognition requires one label image and accepts one optional food image first.");
        }
        for (int index = 0; index < images.size(); index++) {
            AiImageInputBO image = images.get(index);
            if (!isSupportedImage(image)) {
                String role = images.size() == 1 || index == 1 ? "nutrition label" : "food photo";
                throw new BizException(ErrorCode.BAD_REQUEST,
                        "Choose a " + role + " JPEG, PNG, or WebP image smaller than 2.5 MB after compression.");
            }
        }
    }

    private static boolean isSupportedImage(AiImageInputBO image) {
        if (image == null) return false;
        String contentType = normalize(image.getContentType());
        String dataUrl = image.getDataUrl();
        String prefix = "data:" + contentType + ";base64,";
        return FOOD_LABEL_IMAGE_TYPES.contains(contentType)
                && dataUrl != null
                && dataUrl.length() <= MAX_FOOD_LABEL_DATA_URL_LENGTH
                && dataUrl.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
