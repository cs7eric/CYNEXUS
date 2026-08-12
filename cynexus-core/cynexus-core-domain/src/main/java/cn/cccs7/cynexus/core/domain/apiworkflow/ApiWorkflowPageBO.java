package cn.cccs7.cynexus.core.domain.apiworkflow;

import java.util.List;

public record ApiWorkflowPageBO(List<ApiWorkflowProcessBO> items, long total, int page, int pageSize) {
    public ApiWorkflowPageBO {
        items = items == null ? List.of() : List.copyOf(items);
    }

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
