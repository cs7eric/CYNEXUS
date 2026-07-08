package cn.cccs7.cynexus.core.infra.basic.service;

import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.DataSourceHealthCheckRunPO;

public interface DataSourceHealthCheckRunBasicService extends CrudBasicService<DataSourceHealthCheckRunPO> {

    DataSourceHealthCheckRunPO latest();
}
