package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TdxLocalDayKlineClient {

    private static final int RECORD_SIZE = 32;
    private static final BigDecimal PRICE_SCALE = BigDecimal.valueOf(100);
    private final Path tdxRoot;

    public TdxLocalDayKlineClient(@Value("${cynexus.market.tdx.root:D:/Program File/tdx}") String tdxRoot) {
        this.tdxRoot = Path.of(tdxRoot);
    }

    public List<IndustryApplicationBO.Bar> readDailyBars(String symbol, int count) {
        Path path = resolveDayFile(symbol);
        if (!Files.isRegularFile(path)) {
            return List.of();
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length < RECORD_SIZE || bytes.length % RECORD_SIZE != 0) {
                return List.of();
            }
            List<IndustryApplicationBO.Bar> bars = new ArrayList<>();
            for (int offset = 0; offset < bytes.length; offset += RECORD_SIZE) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, RECORD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
                int dateInt = buffer.getInt();
                long openRaw = Integer.toUnsignedLong(buffer.getInt());
                long highRaw = Integer.toUnsignedLong(buffer.getInt());
                long lowRaw = Integer.toUnsignedLong(buffer.getInt());
                long closeRaw = Integer.toUnsignedLong(buffer.getInt());
                float amount = buffer.getFloat();
                long volume = Integer.toUnsignedLong(buffer.getInt());
                buffer.getInt();
                LocalDate tradeDate = parseDate(dateInt);
                bars.add(IndustryApplicationBO.Bar.builder()
                        .tradeDate(tradeDate)
                        .time(tradeDate + " 15:00:00+08:00")
                        .open(price(openRaw))
                        .high(price(highRaw))
                        .low(price(lowRaw))
                        .close(price(closeRaw))
                        .amount(BigDecimal.valueOf(amount))
                        .volumeLots(volume)
                        .build());
            }
            bars.sort(Comparator.comparing(IndustryApplicationBO.Bar::getTradeDate));
            fillPreviousCloseAndPct(bars);
            if (count > 0 && bars.size() > count) {
                return new ArrayList<>(bars.subList(bars.size() - count, bars.size()));
            }
            return bars;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read TDX day file: " + path, ex);
        }
    }

    public List<String> searchLocalDaySymbols(String keyword, int limit) {
        String q = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            return List.of();
        }
        Path vipdoc = tdxRoot.resolve("vipdoc");
        if (!Files.isDirectory(vipdoc)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(vipdoc, 4)) {
            return paths
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".day"))
                    .map(path -> path.getFileName().toString().replace(".day", "").toLowerCase(Locale.ROOT))
                    .filter(name -> name.contains(q) || name.replaceAll("\\D", "").contains(q.replaceAll("\\D", "")))
                    .sorted()
                    .limit(limit)
                    .map(name -> name.replaceAll("\\D", ""))
                    .filter(code -> code.length() == 6)
                    .distinct()
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private Path resolveDayFile(String symbol) {
        String normalized = normalizeSymbol(symbol);
        String market = normalized.substring(0, 2);
        return tdxRoot.resolve("vipdoc").resolve(market).resolve("lday").resolve(normalized + ".day");
    }

    private String normalizeSymbol(String symbol) {
        String value = symbol == null ? "" : symbol.trim().toLowerCase();
        if (value.matches("^[a-z]{2}\\d{6}$")) {
            return value;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 6) {
            if (digits.startsWith("6") || digits.startsWith("5") || digits.startsWith("9") || digits.startsWith("0")) {
                return "sh" + digits;
            }
            return "sz" + digits;
        }
        throw new IllegalArgumentException("Unsupported TDX symbol: " + symbol);
    }

    private LocalDate parseDate(int value) {
        int year = value / 10000;
        int month = (value / 100) % 100;
        int day = value % 100;
        return LocalDate.of(year, month, day);
    }

    private BigDecimal price(long raw) {
        return BigDecimal.valueOf(raw).divide(PRICE_SCALE, 6, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private void fillPreviousCloseAndPct(List<IndustryApplicationBO.Bar> bars) {
        IndustryApplicationBO.Bar previous = null;
        for (IndustryApplicationBO.Bar bar : bars) {
            if (previous != null) {
                bar.setPrevClose(previous.getClose());
                if (previous.getClose() != null && previous.getClose().compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal pct = bar.getClose()
                            .divide(previous.getClose(), 10, RoundingMode.HALF_UP)
                            .subtract(BigDecimal.ONE)
                            .multiply(BigDecimal.valueOf(100));
                    bar.setPct(pct);
                }
            }
            previous = bar;
        }
    }
}
