package cn.cccs7.cynexus.core.application.dto;

/** Binary response prepared by the application layer for the matting endpoint. */
public record FoodImageMattingResponse(byte[] content, String contentType) {

    public FoodImageMattingResponse {
        content = content == null ? new byte[0] : content.clone();
        contentType = contentType == null || contentType.isBlank() ? "image/png" : contentType;
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
