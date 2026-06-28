package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.market.api.dto.MarketPulseIndexReturnsDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseIndexReturnsHistoryDTO;
import cn.cccs7.cynexus.market.domain.entity.IndexReturnDailyBO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MarketPulseIndexReturnItemDTOConverter {

    MarketPulseIndexReturnItemDTOConverter INSTANCE = Mappers.getMapper(MarketPulseIndexReturnItemDTOConverter.class);

    @Mapping(target = "name", expression = "java(indexName(row))")
    @Mapping(target = "code", expression = "java(stripIndexCode(row.getIndexCode()))")
    @Mapping(target = "fullCode", source = "indexCode")
    @Mapping(target = "current", source = "currentClose")
    @Mapping(target = "currentDate", source = "currentTradeDate")
    @Mapping(target = "daily", expression = "java(java.util.List.of())")
    @Mapping(target = "available", constant = "true")
    MarketPulseIndexReturnsDTO.ItemDTO toLatestItem(IndexReturnDailyBO row);

    @Mapping(target = "code", expression = "java(stripIndexCode(row.getIndexCode()))")
    @Mapping(target = "name", expression = "java(indexName(row))")
    @Mapping(target = "current", source = "currentClose")
    @Mapping(target = "currentDate", source = "currentTradeDate")
    MarketPulseIndexReturnsHistoryDTO.ItemDTO toHistoryItem(IndexReturnDailyBO row);

    default List<MarketPulseIndexReturnsDTO.ItemDTO> toLatestItems(List<IndexReturnDailyBO> rows) {
        return rows == null ? List.of() : rows.stream().map(this::toLatestItem).toList();
    }

    default List<MarketPulseIndexReturnsHistoryDTO.ItemDTO> toHistoryItems(List<IndexReturnDailyBO> rows) {
        return rows == null ? List.of() : rows.stream().map(this::toHistoryItem).toList();
    }

    default String stripIndexCode(String code) {
        if (code == null) {
            return "";
        }
        return code.length() > 6 ? code.substring(code.length() - 6) : code;
    }

    default String indexName(IndexReturnDailyBO row) {
        if (row == null) {
            return "";
        }
        String indexName = row.getIndexName();
        return indexName == null || indexName.isBlank() ? stripIndexCode(row.getIndexCode()) : indexName;
    }
}
