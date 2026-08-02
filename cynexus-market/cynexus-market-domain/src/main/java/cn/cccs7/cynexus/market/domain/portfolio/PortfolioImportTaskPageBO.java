package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.List;

public record PortfolioImportTaskPageBO(
        List<PortfolioImportTaskBO> items,
        long total,
        int page,
        int pageSize) {
    public int totalPages() {
        return total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages();
    }
}
