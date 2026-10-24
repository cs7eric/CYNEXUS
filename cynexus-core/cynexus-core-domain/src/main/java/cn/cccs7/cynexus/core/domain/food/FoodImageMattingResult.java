package cn.cccs7.cynexus.core.domain.food;

/** Matting output crossing the domain gateway boundary. */
public record FoodImageMattingResult(byte[] content, String contentType) {

    public FoodImageMattingResult {
        content = content == null ? new byte[0] : content.clone();
        contentType = contentType == null || contentType.isBlank() ? "image/png" : contentType;
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
