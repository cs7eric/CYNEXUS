package cn.cccs7.cynexus.market.domain.portfolio;

public interface PortfolioTradeVisionGateway {
    String parse(PortfolioImportAssetBO asset);

    String parse(String fileName, String contentType, byte[] content);
}
