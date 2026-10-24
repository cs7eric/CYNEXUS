package cn.cccs7.cynexus.common.image;

/** Shared client for the local image matting service. */
public interface ImageMattingClient {

    /**
     * Removes the background from an image and returns the processed image.
     *
     * @param content image bytes
     * @param fileName original file name, used for the multipart part
     * @param contentType original media type
     */
    ImageMattingResult matte(byte[] content, String fileName, String contentType);
}
