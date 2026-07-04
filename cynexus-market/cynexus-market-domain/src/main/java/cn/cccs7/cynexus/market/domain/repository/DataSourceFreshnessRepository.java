package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.DataSourceFreshnessBO;

public interface DataSourceFreshnessRepository {

    DataSourceFreshnessBO snapshot();
}
