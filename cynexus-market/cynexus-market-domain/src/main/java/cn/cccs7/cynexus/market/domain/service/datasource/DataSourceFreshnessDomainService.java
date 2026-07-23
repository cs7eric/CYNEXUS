package cn.cccs7.cynexus.market.domain.service.datasource;

import cn.cccs7.cynexus.market.domain.entity.DataSourceFreshnessBO;

public interface DataSourceFreshnessDomainService {

    DataSourceFreshnessBO snapshot();
}
