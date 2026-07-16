package cn.cccs7.cynexus.market.domain.sentiment;

import cn.cccs7.cynexus.market.domain.entity.StyleRiskDailyBO;
import java.util.List;

public interface StyleRiskDailyDomainService {

    List<StyleRiskDailyBO> list();

    StyleRiskDailyBO findById(Long id);

    Long create(StyleRiskDailyBO value);

    boolean update(StyleRiskDailyBO value);

    boolean delete(Long id);
}
