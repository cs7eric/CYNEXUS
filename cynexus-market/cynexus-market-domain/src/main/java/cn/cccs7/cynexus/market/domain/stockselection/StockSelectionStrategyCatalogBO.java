package cn.cccs7.cynexus.market.domain.stockselection;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class StockSelectionStrategyCatalogBO {

    private StockSelectionStrategyCatalogBO() {
    }

    public record Strategy(long definitionId,
                           String code,
                           String name,
                           String description,
                           String type,
                           Version activeVersion) {
        public Strategy(String code, String name, String description, String type, Version activeVersion) {
            this(0, code, name, description, type, activeVersion);
        }
    }

    public record Version(long id,
                          int versionNo,
                          String parameterSchema,
                          String evidenceSchema,
                          String changeSummary,
                          LocalDate effectiveFrom,
                          long definitionVersion,
                          Long sourceFactorStudyId,
                          Long sourceFactorSetVersionId) {
        public Version(long id, int versionNo, String parameterSchema, String evidenceSchema, String changeSummary,
                       LocalDate effectiveFrom) {
            this(id, versionNo, parameterSchema, evidenceSchema, changeSummary, effectiveFrom, 0, null, null);
        }
    }

    public record Catalog(List<Strategy> items) {
    }

    public record SaveCommand(String name,
                              String description,
                              Map<String, Object> criteria,
                              String changeSummary,
                              Long expectedDefinitionVersion,
                              Long sourceFactorStudyId,
                              Long sourceFactorSetVersionId) {
        public SaveCommand(String name, String description, Map<String, Object> criteria,
                           String changeSummary, Long expectedDefinitionVersion) {
            this(name, description, criteria, changeSummary, expectedDefinitionVersion, null, null);
        }
    }
}
