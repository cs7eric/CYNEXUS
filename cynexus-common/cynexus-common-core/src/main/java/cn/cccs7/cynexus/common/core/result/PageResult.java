package cn.cccs7.cynexus.common.core.result;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long pageNo;
    private long pageSize;
    private long totalPages;
    private boolean hasPrevious;
    private boolean hasNext;

    public static <T> PageResult<T> of(List<T> records, long total, long pageNo, long pageSize) {
        long safePageNo = Math.max(1, pageNo);
        long safePageSize = Math.max(1, pageSize);
        long totalPages = total <= 0 ? 0 : (long) Math.ceil((double) total / safePageSize);
        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .pageNo(safePageNo)
                .pageSize(safePageSize)
                .totalPages(totalPages)
                .hasPrevious(safePageNo > 1)
                .hasNext(totalPages > safePageNo)
                .build();
    }
}
