package cn.cccs7.cynexus.core.domain.finance.entity;

import java.util.List;

public class FinanceAiCaptureHistoryPageBO {
    private List<FinanceAiCaptureBO> items;
    private long total;
    private int limit;
    private int offset;
    private String status;

    public static Builder builder() { return new Builder(); }

    public List<FinanceAiCaptureBO> getItems() { return items; }
    public long getTotal() { return total; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
    public String getStatus() { return status; }

    public static final class Builder {
        private final FinanceAiCaptureHistoryPageBO value = new FinanceAiCaptureHistoryPageBO();
        public Builder items(List<FinanceAiCaptureBO> items) { value.items = items; return this; }
        public Builder total(long total) { value.total = total; return this; }
        public Builder limit(int limit) { value.limit = limit; return this; }
        public Builder offset(int offset) { value.offset = offset; return this; }
        public Builder status(String status) { value.status = status; return this; }
        public FinanceAiCaptureHistoryPageBO build() { return value; }
    }
}
