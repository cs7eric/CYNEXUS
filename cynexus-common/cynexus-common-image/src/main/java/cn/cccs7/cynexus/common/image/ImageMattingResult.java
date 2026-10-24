package cn.cccs7.cynexus.common.image;

/** Binary output returned by an image matting service. */
public record ImageMattingResult(byte[] content, String contentType) {

    public ImageMattingResult {
        content = content == null ? new byte[0] : content.clone();
        contentType = contentType == null || contentType.isBlank() ? "image/png" : contentType;
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

}
