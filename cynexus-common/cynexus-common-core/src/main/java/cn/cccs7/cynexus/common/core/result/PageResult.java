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

    public static <T> PageResult<T> of(List<T> records, long total, long pageNo, long pageSize) {
        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .build();
    }
}
