package cn.cccs7.cynexus.market.infra.stockselection.entity;

import java.time.LocalDate;
import lombok.Data;

@Data
public class StockSelectionStrategyCatalogPO {
    private Long definitionId;
    private Long definitionVersion;
    private String code;
    private String name;
    private String description;
    private String type;
    private Long versionId;
    private Integer versionNo;
    private String parameterSchema;
    private String evidenceSchema;
    private String changeSummary;
    private LocalDate effectiveFrom;
    private Long sourceFactorStudyId;
    private Long sourceFactorSetVersionId;
}
