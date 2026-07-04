package cn.cccs7.cynexus.common.core.result;

import lombok.Getter;

@Getter
public class PageQuery {

    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 500;

    private final int pageNo;
    private final int pageSize;
    private final int offset;

    private PageQuery(int pageNo, int pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.offset = (pageNo - 1) * pageSize;
    }

    public static PageQuery of(Integer pageNo, Integer pageSize) {
        return of(pageNo, pageSize, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
    }

    public static PageQuery of(Integer pageNo, Integer pageSize, int defaultPageSize, int maxPageSize) {
        int safePageNo = pageNo == null || pageNo <= 0 ? DEFAULT_PAGE_NO : pageNo;
        int safeDefaultPageSize = defaultPageSize <= 0 ? DEFAULT_PAGE_SIZE : defaultPageSize;
        int safeMaxPageSize = maxPageSize <= 0 ? MAX_PAGE_SIZE : maxPageSize;
        int requestedPageSize = pageSize == null || pageSize <= 0 ? safeDefaultPageSize : pageSize;
        int safePageSize = Math.max(1, Math.min(requestedPageSize, safeMaxPageSize));
        return new PageQuery(safePageNo, safePageSize);
    }
}
