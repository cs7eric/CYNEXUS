package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationTargetCatalogPO;
import cn.cccs7.cynexus.market.infra.basic.service.IndustryApplicationTargetCatalogBasicService;
import cn.cccs7.cynexus.market.infra.client.ClickHouseStockChartClient;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IndustryApplicationTargetCatalogProvider {

    private static final Charset TDX_CATALOG_CHARSET = Charset.forName("GBK");
    private static final Set<String> TDX_FUND_FLOW_SUPPORTED_CODE6 = Set.of(
            "880301", "880305", "880310", "880318", "880324", "880330", "880335", "880344",
            "880350", "880351", "880355", "880360", "880367", "880372", "880380", "880387",
            "880390", "880398", "880399", "880400", "880406", "880414", "880418", "880421",
            "880422", "880423", "880424", "880430", "880431", "880432", "880437", "880440",
            "880446", "880447", "880448", "880452", "880453", "880454", "880455", "880456",
            "880459", "880464", "880465", "880471", "880472", "880473", "880474", "880476",
            "880482", "880489", "880490", "880491", "880492", "880493", "880494", "880497"
    );

    private final IndustryApplicationTargetCatalogBasicService catalogBasicService;
    private final Path tdxCatalogPath;

    public IndustryApplicationTargetCatalogProvider(
            IndustryApplicationTargetCatalogBasicService catalogBasicService,
            @Value("${cynexus.market.tdx.catalog-file:D:/Program File/tdx/T0002/hq_cache/tdxzs.cfg}") String tdxCatalogFile
    ) {
        this.catalogBasicService = catalogBasicService;
        this.tdxCatalogPath = Path.of(tdxCatalogFile);
    }

    public List<IndustryApplicationBO.TargetCode> mergeAvailable(List<ClickHouseStockChartClient.MarketSymbol> symbols) {
        List<String> normalizedSymbols = normalizeSymbols(symbols);
        ensureCatalogRows(normalizedSymbols);
        Map<String, IndustryApplicationTargetCatalogPO> catalogBySymbol = catalogBasicService.listBySymbols(normalizedSymbols).stream()
                .collect(Collectors.toMap(
                        item -> normalizeSymbol(item.getSymbol()),
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<IndustryApplicationBO.TargetCode> rows = new ArrayList<>();
        for (String symbol : normalizedSymbols) {
            IndustryApplicationTargetCatalogPO catalog = catalogBySymbol.get(symbol);
            rows.add(IndustryApplicationBO.TargetCode.builder()
                    .code(symbol)
                    .name(catalog == null ? symbol : catalog.getName())
                    .kind(catalog == null ? inferKind(symbol) : catalog.getTargetType())
                    .tdxFundFlowSupported(isTdxFundFlowSupported(catalog))
                    .build());
        }
        return rows;
    }

    public Optional<String> lookupName(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        ensureCatalogRows(List.of(normalized));
        return catalogBasicService.findBySymbol(normalized).map(IndustryApplicationTargetCatalogPO::getName);
    }

    public Optional<String> lookupKind(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        ensureCatalogRows(List.of(normalized));
        return catalogBasicService.findBySymbol(normalized).map(IndustryApplicationTargetCatalogPO::getTargetType);
    }

    private void ensureCatalogRows(List<String> symbols) {
        List<String> normalized = symbols.stream()
                .map(this::normalizeSymbol)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return;
        }
        Set<String> existing = catalogBasicService.listBySymbols(normalized).stream()
                .map(IndustryApplicationTargetCatalogPO::getSymbol)
                .filter(Objects::nonNull)
                .map(this::normalizeSymbol)
                .collect(Collectors.toSet());
        List<String> missing = normalized.stream().filter(item -> !existing.contains(item)).toList();
        if (missing.isEmpty()) {
            return;
        }
        List<IndustryApplicationTargetCatalogPO> upserts = new ArrayList<>();
        Set<String> tdxMissingCodes = missing.stream()
                .filter(this::isTdxSectorSymbol)
                .map(item -> item.substring(2))
                .collect(Collectors.toSet());
        if (!tdxMissingCodes.isEmpty()) {
            upserts.addAll(loadTdxCatalogRows(tdxMissingCodes));
        }
        if (!upserts.isEmpty()) {
            catalogBasicService.upsertBatch(upserts);
        }
    }

    private List<IndustryApplicationTargetCatalogPO> loadTdxCatalogRows(Set<String> neededCode6) {
        if (!Files.isRegularFile(tdxCatalogPath)) {
            return List.of();
        }
        try {
            List<IndustryApplicationTargetCatalogPO> rows = new ArrayList<>();
            for (String line : Files.readAllLines(tdxCatalogPath, TDX_CATALOG_CHARSET)) {
                String[] parts = line.split("\\|");
                if (parts.length < 2) {
                    continue;
                }
                String name = parts[0] == null ? "" : parts[0].trim();
                String code6 = parts[1] == null ? "" : parts[1].trim();
                if (!neededCode6.contains(code6)) {
                    continue;
                }
                if (!code6.startsWith("8803") && !code6.startsWith("8804") && !code6.startsWith("8805")) {
                    continue;
                }
                rows.add(buildCatalogRow(resolveTdxTargetType(code6), "sh" + code6, code6, "sh", name, "tdxzs.cfg"));
            }
            return rows;
        } catch (IOException ex) {
            return List.of();
        }
    }

    private List<String> normalizeSymbols(List<ClickHouseStockChartClient.MarketSymbol> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }
        return symbols.stream()
                .map(symbol -> normalizeSymbol((symbol.market() == null ? "" : symbol.market()) + (symbol.symbol() == null ? "" : symbol.symbol())))
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private IndustryApplicationTargetCatalogPO buildCatalogRow(String targetType,
                                                               String symbol,
                                                               String code6,
                                                               String exchange,
                                                               String name,
                                                               String source) {
        IndustryApplicationTargetCatalogPO row = new IndustryApplicationTargetCatalogPO();
        row.setTargetType(targetType);
        row.setSymbol(normalizeSymbol(symbol));
        row.setCode6(code6);
        row.setExchange(exchange);
        row.setName(name);
        row.setSource(source);
        return row;
    }

    private boolean isTdxSectorSymbol(String symbol) {
        return symbol.startsWith("sh8803") || symbol.startsWith("sh8804") || symbol.startsWith("sh8805");
    }

    private String resolveTdxTargetType(String code6) {
        if (code6 != null && code6.startsWith("8805")) {
            return "concept";
        }
        return "industry";
    }

    private String inferKind(String symbol) {
        if (symbol.startsWith("sh000") || symbol.startsWith("sz399")) {
            return "index";
        }
        return "industry";
    }

    private boolean isTdxFundFlowSupported(IndustryApplicationTargetCatalogPO catalog) {
        if (catalog == null) {
            return false;
        }
        if (!"industry".equalsIgnoreCase(catalog.getTargetType())) {
            return false;
        }
        String code6 = catalog.getCode6();
        return code6 != null && TDX_FUND_FLOW_SUPPORTED_CODE6.contains(code6.trim());
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toLowerCase(Locale.ROOT);
    }
}
