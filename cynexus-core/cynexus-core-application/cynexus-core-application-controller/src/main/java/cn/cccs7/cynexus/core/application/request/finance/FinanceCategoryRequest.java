package cn.cccs7.cynexus.core.application.request.finance;

import lombok.Data;

@Data
public class FinanceCategoryRequest {
    private Long id;
    private Long parentId;
    private String name;
    private String direction;
    private String icon;
    private String color;
    private Integer sortOrder;
}
