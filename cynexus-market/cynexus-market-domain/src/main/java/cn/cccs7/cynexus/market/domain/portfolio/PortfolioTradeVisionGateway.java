package cn.cccs7.cynexus.market.domain.portfolio;

public interface PortfolioTradeVisionGateway {
    String parse(PortfolioImportAssetBO asset);

    default String review(PortfolioImportAssetBO asset, String extractedResult) {
        throw new UnsupportedOperationException("trade screenshot AI review is not configured");
    }

    String parse(String fileName, String contentType, byte[] content);
}
