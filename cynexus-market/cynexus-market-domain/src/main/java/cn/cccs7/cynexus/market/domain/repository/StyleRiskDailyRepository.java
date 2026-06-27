package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.StyleRiskDailyBO;
import java.util.List;

public interface StyleRiskDailyRepository {

    List<StyleRiskDailyBO> list();

    StyleRiskDailyBO findById(Long id);

    Long create(StyleRiskDailyBO value);

    boolean update(StyleRiskDailyBO value);

    boolean delete(Long id);
}
