package cn.cccs7.cynexus.market.domain.information;

public interface StockInformationAnalysisGateway {

    AnalysisResult analyze(String symbol, String title, String content);

    record AnalysisResult(String modelCode, String resultJson) {
    }
}
