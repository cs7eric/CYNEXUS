package cn.cccs7.cynexus.market.domain.service.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.cccs7.cynexus.market.domain.entity.AStockSkillDataBO;
import cn.cccs7.cynexus.market.domain.repository.AStockSkillDataRepository;
import cn.cccs7.cynexus.market.domain.service.stock.impl.AStockSkillDataQueryDomainServiceImpl;
import org.junit.jupiter.api.Test;

class AStockSkillDataQueryDomainServiceTest {

    @Test
    void normalizesRowsQuery() {
        String[] dataset = new String[1];
        int[] limit = new int[1];
        AStockSkillDataRepository repository = new AStockSkillDataRepository() {
            @Override public AStockSkillDataBO.DatasetListBO listDatasets() { return null; }
            @Override public AStockSkillDataBO.RowsBO listRows(String requestedDataset, Integer requestedLimit, String code, String source) {
                dataset[0] = requestedDataset;
                limit[0] = requestedLimit;
                return null;
            }
        };

        new AStockSkillDataQueryDomainServiceImpl(repository).listRows(" daily ", 9999, " 600519 ", " tdx ");

        assertEquals("daily", dataset[0]);
        assertEquals(1000, limit[0]);
    }
}
